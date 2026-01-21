package com.example.babyneedscounter

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import android.view.HapticFeedbackConstants
import android.view.View

/**
 * Utility for providing haptic feedback throughout the app
 */
object HapticFeedback {
    
    /**
     * Light tap feedback for button presses
     */
    fun lightTap(context: Context) {
        performHaptic(context, 10)
    }
    
    /**
     * Medium impact for logging events
     */
    fun mediumImpact(context: Context) {
        performHaptic(context, 50)
    }
    
    /**
     * Strong press feedback for button presses with satisfying "click" feel
     */
    fun buttonPress(context: Context) {
        performPattern(context, longArrayOf(0, 30, 10, 20))
    }
    
    /**
     * Success feedback (double tap pattern)
     */
    fun success(context: Context) {
        performPattern(context, longArrayOf(0, 50, 50, 100))
    }
    
    /**
     * Error feedback (short intense vibration)
     */
    fun error(context: Context) {
        performHaptic(context, 200)
    }
    
    /**
     * Perform haptic feedback with specified duration
     */
    private fun performHaptic(context: Context, durationMs: Long) {
        try {
            val vibrator = getVibrator(context)
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Use VibrationEffect for API 26+
                val effect = VibrationEffect.createOneShot(
                    durationMs,
                    VibrationEffect.DEFAULT_AMPLITUDE
                )
                vibrator.vibrate(effect)
            } else {
                // Fallback for older devices
                @Suppress("DEPRECATION")
                vibrator.vibrate(durationMs)
            }
            
            Log.d("HapticFeedback", "Performed haptic feedback: ${durationMs}ms")
        } catch (e: Exception) {
            Log.e("HapticFeedback", "Error performing haptic feedback", e)
        }
    }
    
    /**
     * Perform pattern-based haptic feedback
     */
    private fun performPattern(context: Context, pattern: LongArray) {
        try {
            val vibrator = getVibrator(context)
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Use VibrationEffect for API 26+
                val amplitudes = IntArray(pattern.size) { 
                    if (pattern[it] > 0) VibrationEffect.DEFAULT_AMPLITUDE else 0 
                }
                val effect = VibrationEffect.createWaveform(pattern, amplitudes, -1)
                vibrator.vibrate(effect)
            } else {
                // Fallback for older devices
                @Suppress("DEPRECATION")
                vibrator.vibrate(pattern, -1)
            }
            
            Log.d("HapticFeedback", "Performed pattern haptic feedback")
        } catch (e: Exception) {
            Log.e("HapticFeedback", "Error performing pattern haptic feedback", e)
        }
    }
    
    /**
     * Get the vibrator service
     */
    private fun getVibrator(context: Context): Vibrator {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }
    
    /**
     * Check if device supports haptic feedback
     */
    fun isSupported(context: Context): Boolean {
        return try {
            val vibrator = getVibrator(context)
            vibrator.hasVibrator()
        } catch (e: Exception) {
            false
        }
    }
}
