package com.example.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import java.util.Random
import kotlin.math.sin

enum class SkyState {
    MORNING,
    MIDDAY,
    SUNSET,
    NIGHT,
    STORM,
    AURORA;

    val gradientColors: Pair<Color, Color>
        get() = when (this) {
            MORNING -> Pair(Color(0xFFE07A5F), Color(0xFFF4F1DE)) // Coral sunrise to soft golden cream
            MIDDAY -> Pair(Color(0xFF0288D1), Color(0xFFB3E5FC))  // Sky blue to light cream blue
            SUNSET -> Pair(Color(0xFF81173C), Color(0xFFF28482))  // Deep wine red to warm peach orange
            NIGHT -> Pair(Color(0xFF03071E), Color(0xFF130A21))   // Cosmic dark to midnight violet
            STORM -> Pair(Color(0xFF2B2D42), Color(0xFF5C677D))   // Storm gray-dark to wet slate blue
            AURORA -> Pair(Color(0xFF0A0118), Color(0xFF16012E))  // Pure space darkness to violet glow
        }
}

// Fixed positions for stars, clouds, and raindrops to prevent reshuffling on recomposition
private class SkyElementsState {
    val starPositions = List(30) {
        Offset(Random().nextFloat(), Random().nextFloat())
    }
    val cloudOffsets = List(4) {
        Pair(Random().nextFloat() * 200f, Random().nextFloat() * 400f + 100f)
    }
    val rainOffsets = List(40) {
        Offset(Random().nextFloat(), Random().nextFloat())
    }
}

@Composable
fun SkyCanvas(
    skyState: SkyState,
    modifier: Modifier = Modifier
) {
    // Smooth transitions for background gradient colors
    val startColor by animateColorAsState(
        targetValue = skyState.gradientColors.first,
        animationSpec = tween(durationMillis = 1500, easing = LinearOutSlowInEasing),
        label = "StartColor"
    )
    val endColor by animateColorAsState(
        targetValue = skyState.gradientColors.second,
        animationSpec = tween(durationMillis = 1500, easing = LinearOutSlowInEasing),
        label = "EndColor"
    )

    // Elements state
    val elements = remember { SkyElementsState() }

    // Infinite transitions for animating visual effects
    val infiniteTransition = rememberInfiniteTransition(label = "SkyAnimations")

    // Stars twinkling factor
    val starAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "StarTwinkle"
    )

    // Clouds horizontal movement
    val cloudAnimOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(40000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "CloudFloat"
    )

    // Rain vertical animation
    val rainAnimOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "RainFall"
    )

    // Aurora wave amplitude animation
    val auroraPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "AuroraWave"
    )

    // Storm lightning trigger (intermittent flash)
    var showLightning by remember { mutableStateOf(false) }
    LaunchedEffect(skyState) {
        if (skyState == SkyState.STORM) {
            while (true) {
                kotlinx.coroutines.delay(Random().nextLong(3000, 8000))
                // Quick double flash
                showLightning = true
                kotlinx.coroutines.delay(80)
                showLightning = false
                kotlinx.coroutines.delay(120)
                showLightning = true
                kotlinx.coroutines.delay(60)
                showLightning = false
            }
        } else {
            showLightning = false
        }
    }

    val lightningOverlayColor by animateColorAsState(
        targetValue = if (showLightning) Color(0xAAFFFFFF) else Color.Transparent,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessHigh),
        label = "LightningColor"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .drawBehind {
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(startColor, endColor)
                    )
                )
                if (lightningOverlayColor != Color.Transparent) {
                    drawRect(color = lightningOverlayColor)
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            // 1. Draw Twinkling Stars (for NIGHT and AURORA)
            if (skyState == SkyState.NIGHT || skyState == SkyState.AURORA) {
                elements.starPositions.forEachIndexed { index, star ->
                    val x = star.x * width
                    val y = star.y * height * 0.7f // confine stars to upper 70% of the screen
                    val individualTwinkle = if (index % 2 == 0) starAlpha else (1.2f - starAlpha)
                    drawCircle(
                        color = Color.White.copy(alpha = individualTwinkle.coerceIn(0.1f, 1f)),
                        radius = (1.5f + (index % 3)).dp.toPx(),
                        center = Offset(x, y)
                    )
                }
            }

            // 2. Draw Aurora ribbons (for AURORA)
            if (skyState == SkyState.AURORA) {
                val path = Path()
                val path2 = Path()
                
                val startY1 = height * 0.25f
                val startY2 = height * 0.35f
                
                path.moveTo(0f, startY1)
                path2.moveTo(0f, startY2)

                for (x in 0..width.toInt() step 20) {
                    val angle = (x.toFloat() / width) * 4f * Math.PI.toFloat() + auroraPhase
                    val y1 = startY1 + sin(angle) * 60f
                    val y2 = startY2 + sin(angle + 1f) * 80f
                    path.lineTo(x.toFloat(), y1)
                    path2.lineTo(x.toFloat(), y2)
                }

                drawPath(
                    path = path,
                    color = Color(0xFF00F5D4).copy(alpha = 0.35f),
                    style = Stroke(width = 30.dp.toPx())
                )
                drawPath(
                    path = path2,
                    color = Color(0xFF70E000).copy(alpha = 0.25f),
                    style = Stroke(width = 45.dp.toPx())
                )
            }

            // 3. Draw Floating Clouds (for MIDDAY and MORNING)
            if (skyState == SkyState.MIDDAY || skyState == SkyState.MORNING) {
                val cloudColor = if (skyState == SkyState.MORNING) {
                    Color(0x99FFECE6) // warm tinted clouds
                } else {
                    Color(0xCCFFFFFF) // bright white clouds
                }

                elements.cloudOffsets.forEachIndexed { index, cloud ->
                    val baseSpeed = (1f + index * 0.5f)
                    val x = ((cloud.first + cloudAnimOffset * baseSpeed) % (width + 300f)) - 150f
                    val y = cloud.second

                    // Draw a cloud puff (overlapping circles)
                    val radius = (40f + index * 10f).dp.toPx()
                    drawCircle(color = cloudColor, radius = radius, center = Offset(x, y))
                    drawCircle(color = cloudColor, radius = radius * 0.8f, center = Offset(x - radius * 0.6f, y + radius * 0.1f))
                    drawCircle(color = cloudColor, radius = radius * 0.8f, center = Offset(x + radius * 0.6f, y + radius * 0.1f))
                }
            }

            // 4. Draw Falling Raindrops (for STORM)
            if (skyState == SkyState.STORM) {
                elements.rainOffsets.forEachIndexed { index, rain ->
                    val xStart = rain.x * width
                    // Slow/fast rain variation
                    val speedFactor = 1f + (index % 4) * 0.3f
                    val yStart = ((rain.y + rainAnimOffset * speedFactor) % 1f) * height
                    
                    // Rain streaks (slanted to simulate wind)
                    val xEnd = xStart - 10.dp.toPx()
                    val yEnd = yStart + 20.dp.toPx()

                    drawLine(
                        color = Color(0x66B0C4DE),
                        start = Offset(xStart, yStart),
                        end = Offset(xEnd, yEnd),
                        strokeWidth = (1.5f + (index % 2)).dp.toPx()
                    )
                }
            }
        }
    }
}
