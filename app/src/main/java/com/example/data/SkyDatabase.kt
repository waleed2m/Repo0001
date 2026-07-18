package com.example.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "chat_messages")
data class ChatMessage(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val skyState: String // e.g. "MORNING", "MIDDAY", "SUNSET", "NIGHT", "STORM", "AURORA"
)

@Dao
interface ChatMessageDao {
    @Query("SELECT * FROM chat_messages ORDER BY timestamp ASC")
    fun getAllMessages(): Flow<List<ChatMessage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessage)

    @Query("DELETE FROM chat_messages")
    suspend fun clearHistory()
}

@Database(entities = [ChatMessage::class], version = 1, exportSchema = false)
abstract class SkyDatabase : RoomDatabase() {
    abstract fun chatMessageDao(): ChatMessageDao

    companion object {
        @Volatile
        private var INSTANCE: SkyDatabase? = null

        fun getDatabase(context: Context): SkyDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SkyDatabase::class.java,
                    "sky_chat_database"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}

class ChatRepository(private val chatMessageDao: ChatMessageDao) {
    val allMessages: Flow<List<ChatMessage>> = chatMessageDao.getAllMessages()

    suspend fun insertMessage(message: ChatMessage) {
        chatMessageDao.insertMessage(message)
    }

    suspend fun clearHistory() {
        chatMessageDao.clearHistory()
    }
}
