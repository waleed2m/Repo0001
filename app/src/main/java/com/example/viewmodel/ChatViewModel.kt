package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.*
import com.example.ui.SkyState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val database = SkyDatabase.getDatabase(application)
    private val repository = ChatRepository(database.chatMessageDao())

    // All persisted chat messages from DB
    val messages: StateFlow<List<ChatMessage>> = repository.allMessages
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Current Sky State (controlled by inputs or responses)
    private val _currentSkyState = MutableStateFlow(SkyState.MIDDAY)
    val currentSkyState: StateFlow<SkyState> = _currentSkyState.asStateFlow()

    // Loading indicator for API calls
    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    // Initialize with a welcome message if the database is empty
    init {
        viewModelScope.launch {
            repository.allMessages.first().let { currentList ->
                if (currentList.isEmpty()) {
                    // Send an initial welcome message from the Sky Spirit
                    repository.insertMessage(
                        ChatMessage(
                            text = "Hello, traveler of the earth! I am the Sky Spirit. Type 'good morning', 'good night', 'sunset vibe', or tell me how you are feeling to see my sky colors transform.",
                            isUser = false,
                            skyState = "MIDDAY"
                        )
                    )
                } else {
                    // Set sky state to the last message's state
                    val lastMsg = currentList.lastOrNull()
                    if (lastMsg != null) {
                        try {
                            _currentSkyState.value = SkyState.valueOf(lastMsg.skyState)
                        } catch (e: Exception) {
                            _currentSkyState.value = SkyState.MIDDAY
                        }
                    }
                }
            }
        }
    }

    // Check if the API key is valid (i.e. not the placeholder string)
    private fun isApiKeyValid(): Boolean {
        val key = BuildConfig.GEMINI_API_KEY
        return key.isNotEmpty() && 
               key != "MY_GEMINI_API_KEY" && 
               key != "GEMINI_API_KEY" && 
               !key.contains("PLACEHOLDER")
    }

    fun sendMessage(text: String) {
        if (text.isBlank()) return

        val userSkyStateStr = _currentSkyState.value.name

        viewModelScope.launch {
            // 1. Instantly insert user message into database
            val userMsg = ChatMessage(text = text, isUser = true, skyState = userSkyStateStr)
            repository.insertMessage(userMsg)

            // 2. Perform instant local analysis to transition the background sky state immediately (makes it feel highly responsive)
            val matchedState = detectSkyStateFromText(text)
            if (matchedState != null) {
                _currentSkyState.value = matchedState
            }

            // 3. Get response from either Gemini or the local chatbot
            _isGenerating.value = true
            try {
                if (isApiKeyValid()) {
                    getGeminiResponse(text)
                } else {
                    // Fallback to local poetic chatbot
                    getLocalResponse(text)
                }
            } catch (e: Exception) {
                // If anything fails, fall back to local response
                getLocalResponse(text)
            } finally {
                _isGenerating.value = false
            }
        }
    }

    fun clearChatHistory() {
        viewModelScope.launch {
            repository.clearHistory()
            _currentSkyState.value = SkyState.MIDDAY
            // Insert initial message again
            repository.insertMessage(
                ChatMessage(
                    text = "Hello, traveler! I've cleared our memories, but I am ready to start a new day. Tell me, how are you feeling?",
                    isUser = false,
                    skyState = "MIDDAY"
                )
            )
        }
    }

    // Helper to transition sky state programmatically via actions/chips
    fun setSkyStateDirectly(state: SkyState) {
        _currentSkyState.value = state
        val textPrompt = when (state) {
            SkyState.MORNING -> "Rise and shine! Show me your sunrise colors."
            SkyState.MIDDAY -> "Bright day! Let the blue sky clear my mind."
            SkyState.SUNSET -> "Good evening. Show me your sunset colors."
            SkyState.NIGHT -> "Good night. Make it starry and dark."
            SkyState.STORM -> "Let it rain. Show me your dark stormy sky."
            SkyState.AURORA -> "Let the cosmos dance. Show me your auroras."
        }
        sendMessage(textPrompt)
    }

    private suspend fun getGeminiResponse(userPrompt: String) {
        withContext(Dispatchers.IO) {
            // Gather last 10 messages for simple context
            val history = messages.value.takeLast(10)
            val contents = history.map { msg ->
                Content(parts = listOf(Part(text = if (msg.isUser) msg.text else msg.text)))
            }

            val systemInstruction = Content(
                parts = listOf(Part(text = """
                    You are the Sky Spirit, a friendly and magical celestial companion that lives within the sky colors.
                    Respond to the user with a comforting, poetic, short message (maximum 2-3 sentences).
                    Along with your text response, select the sky state that best fits the user's input, context, or mood.
                    The sky states you can choose are:
                    - MORNING (for morning greetings, sunrise, freshness, beginnings, waking up)
                    - MIDDAY (for brightness, clear skies, work, afternoons, active energy, logic)
                    - SUNSET (for evening greetings, twilight, relaxation, orange/pink moods, beauty, winding down)
                    - NIGHT (for bedtime, stars, sleep, dreams, quiet, introspection, mystery)
                    - STORM (for sadness, anger, rainy days, electricity, power, dramatic moods, heavy feelings)
                    - AURORA (for wonder, magic, Northern lights, cosmic mystery, dreams, neon colors, rare feelings)

                    You MUST strictly return a JSON object containing EXACTLY these two keys:
                    {
                      "response": "your poetic message",
                      "skyState": "MORNING|MIDDAY|SUNSET|NIGHT|STORM|AURORA"
                    }
                """.trimIndent()))
            )

            val request = GenerateContentRequest(
                contents = contents,
                generationConfig = GenerationConfig(
                    responseFormat = ResponseFormat(text = ResponseFormatText(mimeType = "application/json")),
                    temperature = 0.7f
                ),
                systemInstruction = systemInstruction
            )

            try {
                val apiKey = BuildConfig.GEMINI_API_KEY
                val apiResponse = RetrofitClient.service.generateContent(apiKey, request)
                val rawText = apiResponse.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                
                if (rawText != null) {
                    val sanitizedJson = sanitizeJson(rawText)
                    val parsed = RetrofitClient.responseAdapter.fromJson(sanitizedJson)
                    if (parsed != null) {
                        val finalSkyState = try {
                            SkyState.valueOf(parsed.skyState.uppercase().trim())
                        } catch (e: Exception) {
                            detectSkyStateFromText(parsed.response) ?: _currentSkyState.value
                        }

                        // Update current state
                        _currentSkyState.value = finalSkyState

                        // Persistent insert
                        repository.insertMessage(
                            ChatMessage(
                                text = parsed.response,
                                isUser = false,
                                skyState = finalSkyState.name
                            )
                        )
                        return@withContext
                    }
                }
                // Fallback inside Gemini error
                getLocalResponse(userPrompt)
            } catch (e: Exception) {
                getLocalResponse(userPrompt)
            }
        }
    }

    private fun sanitizeJson(raw: String): String {
        var clean = raw.trim()
        if (clean.startsWith("```json")) {
            clean = clean.removePrefix("```json")
        } else if (clean.startsWith("```")) {
            clean = clean.removePrefix("```")
        }
        if (clean.endsWith("```")) {
            clean = clean.removeSuffix("```")
        }
        return clean.trim()
    }

    private suspend fun getLocalResponse(text: String) {
        val lower = text.lowercase()
        val responseText: String
        val targetState: SkyState

        when {
            // MORNING keywords
            lower.contains("morning") || lower.contains("sunrise") || lower.contains("wake") || lower.contains("dawn") || lower.contains("shine") -> {
                targetState = SkyState.MORNING
                responseText = listOf(
                    "Good morning, traveler of the earth. Let the golden sunrise fill you with fresh hope and light.",
                    "The dawn rises in gentle coral and gold. A brand new day has dawned for you to explore.",
                    "Good morning! I've painted the sky in soft shades of gold and morning dew to start your day beautifully."
                ).random()
            }
            // NIGHT keywords
            lower.contains("night") || lower.contains("dark") || lower.contains("sleep") || lower.contains("bed") || lower.contains("dream") || lower.contains("starry") || lower.contains("stars") || lower.contains("goodnight") -> {
                targetState = SkyState.NIGHT
                responseText = listOf(
                    "Good night, tired soul. The stars are twinkling gently in my dark blanket. May you have peaceful dreams.",
                    "A quiet canopy of violet and indigo settles over the world. Rest your thoughts and let the night carry your worries away.",
                    "Good night! I've lit up my stars to watch over you as you drift off into deep, gentle slumber."
                ).random()
            }
            // SUNSET keywords
            lower.contains("sunset") || lower.contains("evening") || lower.contains("dusk") || lower.contains("twilight") || lower.contains("relax") || lower.contains("wind down") -> {
                targetState = SkyState.SUNSET
                responseText = listOf(
                    "Good evening. See how I paint my clouds in fiery rose and magenta as the day rests. Breathe out and let go.",
                    "Twilight is here, blending warm crimson and soft gold. It is the perfect hour to find peace and unwind.",
                    "The day is taking its final bow. Sit with me and appreciate this beautiful, calming sunset."
                ).random()
            }
            // STORM keywords
            lower.contains("storm") || lower.contains("rain") || lower.contains("thunder") || lower.contains("angry") || lower.contains("sad") || lower.contains("cry") || lower.contains("heavy") || lower.contains("lightning") -> {
                targetState = SkyState.STORM
                responseText = listOf(
                    "The heavy slate gray clouds gather. It is okay to release your storm—even the sky has to rain before it clears.",
                    "Thunder rolls and rain falls. Let my gray clouds shelter you; after the heaviest storm, the earth always blooms.",
                    "Let it pour. There is a deep, raw strength in a storm. I will hold the lightning for you until you are ready."
                ).random()
            }
            // AURORA keywords
            lower.contains("aurora") || lower.contains("magic") || lower.contains("northern lights") || lower.contains("cosmic") || lower.contains("wonder") || lower.contains("neon") || lower.contains("beautiful") -> {
                targetState = SkyState.AURORA
                responseText = listOf(
                    "The solar winds whisper, and cosmic ribbons of green and violet dance across the sky. Let the aurora ignite your wonder.",
                    "Behold the magical glow of the northern lights! You are witnessing a rare, cosmic masterpiece written in light.",
                    "The universe is dancing in curtains of emerald and amethyst. Remember, you contain that same magic inside of you."
                ).random()
            }
            // MIDDAY keywords or defaults
            lower.contains("afternoon") || lower.contains("noon") || lower.contains("midday") || lower.contains("clear") || lower.contains("sun") || lower.contains("work") || lower.contains("bright") || lower.contains("blue") -> {
                targetState = SkyState.MIDDAY
                responseText = listOf(
                    "The sun stands tall in my clear, bright blue skies. What a perfect day to breathe deeply and take a step forward.",
                    "Under this brilliant blue azure, everything is crisp and full of life. Keep shining!",
                    "A clear midday sky has opened up for you. Let the bright azure wash away any fog in your thoughts."
                ).random()
            }
            else -> {
                // Keep current sky state or make it random, default message
                targetState = _currentSkyState.value
                responseText = listOf(
                    "Hello! I am the Sky Spirit, watching over your journey. Speak to me, or say 'good morning', 'sunset', or 'make it storm' to change my colors.",
                    "I am listening. Whether your heart feels sunny, stormy, or full of wonder, I will paint my colors to match.",
                    "Tell me more, earth traveler. I am happy to paint whatever sky you need right now."
                ).random()
            }
        }

        // Apply state and insert message
        _currentSkyState.value = targetState
        repository.insertMessage(
            ChatMessage(
                text = responseText,
                isUser = false,
                skyState = targetState.name
            )
        )
    }

    private fun detectSkyStateFromText(text: String): SkyState? {
        val lower = text.lowercase()
        return when {
            lower.contains("morning") || lower.contains("sunrise") || lower.contains("wake") || lower.contains("dawn") || lower.contains("shine") -> SkyState.MORNING
            lower.contains("night") || lower.contains("dark") || lower.contains("sleep") || lower.contains("bed") || lower.contains("dream") || lower.contains("starry") || lower.contains("stars") || lower.contains("goodnight") -> SkyState.NIGHT
            lower.contains("sunset") || lower.contains("evening") || lower.contains("dusk") || lower.contains("twilight") || lower.contains("relax") || lower.contains("wind down") -> SkyState.SUNSET
            lower.contains("storm") || lower.contains("rain") || lower.contains("thunder") || lower.contains("angry") || lower.contains("sad") || lower.contains("cry") || lower.contains("heavy") || lower.contains("lightning") -> SkyState.STORM
            lower.contains("aurora") || lower.contains("magic") || lower.contains("northern lights") || lower.contains("cosmic") || lower.contains("wonder") || lower.contains("neon") || lower.contains("beautiful") -> SkyState.AURORA
            lower.contains("afternoon") || lower.contains("noon") || lower.contains("midday") || lower.contains("clear") || lower.contains("sun") || lower.contains("work") || lower.contains("bright") || lower.contains("blue") -> SkyState.MIDDAY
            else -> null
        }
    }
}
