package com.innojane.joyconinkturner.input

/**
 * Swipe configuration for page turning.
 *
 * Ratios are relative to screen width/height.
 * Tune these values for different devices (e.g. Boox Palma 2).
 */
object SwipeConfig {

    // Start X position as % of screen width
    const val START_X_RATIO = 0.80f

    // End X position as % of screen width
    const val END_X_RATIO = 0.20f

    // Y position as % of screen height (middle of screen)
    const val Y_RATIO = 0.50f

    // Swipe duration in ms
    const val DURATION_MS = 120L
}