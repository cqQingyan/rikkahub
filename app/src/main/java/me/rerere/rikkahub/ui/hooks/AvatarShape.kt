package me.rerere.rikkahub.ui.hooks

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Shape
import kotlin.math.roundToInt

@Composable
fun rememberAvatarShape(loading: Boolean): Shape {
    val infiniteTransition = rememberInfiniteTransition()
    val rotateAngle = infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 3000,
                easing = LinearEasing
            ),
        )
    )
    return if (loading) MaterialShapes.Cookie6Sided.toShape(rotateAngle.value.roundToInt()) else CircleShape
}

@Composable
fun rememberAvatarBreathing(loading: Boolean): Float {
    if (!loading) return 1f
    val infiniteTransition = rememberInfiniteTransition()
    val scale = infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1000,
                easing = androidx.compose.animation.core.FastOutSlowInEasing
            ),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        )
    )
    return scale.value
}
