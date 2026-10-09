package com.pocketsparrow.ui.theme

import android.provider.Settings
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * Pocket Sparrow Enterprise Motion Design System
 * 
 * Strict motion tokens matching the desktop React Framer Motion system:
 * - Easing: cubic-bezier(0.4, 0, 0.2, 1)
 * - Micro: 150ms (press, toggle, micro-interactions)
 * - Macro: 300ms (drawer, tab, page transitions)
 * - Threat: 400ms (threat detection alert / spring feedback)
 * - Stagger: 50ms per item
 */
object MotionTokens {
    // Easing tokens
    val StandardEasing: Easing = CubicBezierEasing(0.4f, 0.0f, 0.2f, 1.0f)
    
    // Duration tokens (ms)
    object Duration {
        const val Micro = 150
        const val Macro = 300
        const val Threat = 400
        const val Stagger = 50
    }

    // Standard tweens using cubic-bezier(0.4, 0, 0.2, 1)
    fun <T> microTween(delayMillis: Int = 0) = tween<T>(
        durationMillis = Duration.Micro,
        delayMillis = delayMillis,
        easing = StandardEasing
    )

    fun <T> macroTween(delayMillis: Int = 0) = tween<T>(
        durationMillis = Duration.Macro,
        delayMillis = delayMillis,
        easing = StandardEasing
    )

    // Threat spring feedback
    fun <T> threatSpring() = spring<T>(
        dampingRatio = 0.72f,
        stiffness = Spring.StiffnessMedium
    )
}

/**
 * Hook to detect whether the user has enabled "Remove animations" / reduced motion
 * in Android system accessibility settings. Degrades gracefully to instantaneous or
 * simple opacity transitions when true.
 */
@Composable
fun isReducedMotion(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        try {
            val scale = Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1.0f
            )
            scale == 0f
        } catch (_: Exception) {
            false
        }
    }
}
