package com.innojane.joyconinkturner.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.annotation.SuppressLint
import android.content.SharedPreferences
import android.graphics.Path
import android.util.Log
import android.view.InputDevice
import android.view.KeyEvent
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import com.innojane.joyconinkturner.config.Prefs
import com.innojane.joyconinkturner.input.JoyConKeyMap

@SuppressLint("AccessibilityPolicy")
class PageTurnerService : AccessibilityService() {

    @Volatile
    private var pageTurnerEnabled = true

    private val prefs by lazy {
        applicationContext.getSharedPreferences(Prefs.FILE_NAME, MODE_PRIVATE)
    }

    private val prefListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == Prefs.KEY_ENABLED) {
            pageTurnerEnabled = prefs.getBoolean(Prefs.KEY_ENABLED, true)
        }
    }

    private var cachedSize: Pair<Float, Float>? = null

    private enum class PendingTap { LEFT, RIGHT }

    @Volatile
    private var gestureInFlight = false

    @Volatile
    private var pendingTap: PendingTap? = null


    private var lastKeyDownAt = 0L
    private var lastDispatchAt = 0L

    private fun isPageTurnerEnabled(): Boolean {
        val prefs = getSharedPreferences("page_turner_prefs", MODE_PRIVATE)
        return prefs.getBoolean("page_turner_enabled", true)
    }

    private val gestureCallback = object : GestureResultCallback() {
        override fun onCompleted(gestureDescription: GestureDescription?) {
            val now = android.os.SystemClock.uptimeMillis()
            Log.d(
                TAG,
                "latency input->dispatch=${lastDispatchAt - lastKeyDownAt}ms, dispatch->complete=${now - lastDispatchAt}ms"
            )
            gestureInFlight = false
            val next = pendingTap
            pendingTap = null
            if (next != null) startTap(next)
        }

        override fun onCancelled(gestureDescription: GestureDescription?) {
            gestureInFlight = false
            pendingTap = null
        }
    }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        if (!isPageTurnerEnabled()) return false
        if (event.action != KeyEvent.ACTION_DOWN) return false
        if (!isGamepadSource(event.source)) return false
        if (event.action == KeyEvent.ACTION_DOWN) {
            lastKeyDownAt = android.os.SystemClock.uptimeMillis()
        }

        return when (event.keyCode) {
            JoyConKeyMap.JOYCON_KEYCODE_DPAD_UP,
            JoyConKeyMap.JOYCON_KEYCODE_DPAD_LEFT,
            JoyConKeyMap.JOYCON_KEYCODE_BUTTON_Y,
            JoyConKeyMap.JOYCON_KEYCODE_BUTTON_X -> {
                requestTapLeft(); true
            }

            JoyConKeyMap.JOYCON_KEYCODE_DPAD_DOWN,
            JoyConKeyMap.JOYCON_KEYCODE_DPAD_RIGHT,
            JoyConKeyMap.JOYCON_KEYCODE_BUTTON_A,
            JoyConKeyMap.JOYCON_KEYCODE_BUTTON_B -> {
                requestTapRight(); true
            }

            else -> false
        }
    }

    private fun screenSize(): Pair<Float, Float> {
        cachedSize?.let { return it }

        val wm = getSystemService(WindowManager::class.java)
        val bounds = wm.currentWindowMetrics.bounds
        val size = bounds.width().toFloat() to bounds.height().toFloat()

        cachedSize = size
        return size
    }


    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        if (event.packageName?.toString() != "com.tencent.weread.eink") return

        Log.d(
            TAG,
            "A11Y type=${event.eventType} class=${event.className} " +
                    "text=${event.text} contentDesc=${event.contentDescription}"
        )
    }

    override fun onInterrupt() {
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        invalidateScreenSizeCache()
        pageTurnerEnabled = prefs.getBoolean(Prefs.KEY_ENABLED, true)
        prefs.registerOnSharedPreferenceChangeListener(prefListener)
        Log.d(TAG, "PageTurnerService connected!")
    }

    override fun onDestroy() {
        prefs.unregisterOnSharedPreferenceChangeListener(prefListener)
        super.onDestroy()
    }

    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        super.onConfigurationChanged(newConfig)
        invalidateScreenSizeCache()
        Log.d(TAG, "Configuration changed → invalidate screen size cache")
    }

    private fun invalidateScreenSizeCache() {
        cachedSize = null
    }

    private fun requestTapLeft() {
        enqueueTap(PendingTap.LEFT)
    }

    private fun requestTapRight() {
        enqueueTap(PendingTap.RIGHT)
    }

    private fun enqueueTap(tap: PendingTap) {
        if (gestureInFlight) {
            pendingTap = tap // 只保留最后一次
            return
        }
        startTap(tap)
    }

    private fun startTap(tap: PendingTap) {
        gestureInFlight = true
        val (w, h) = screenSize()
        val x = if (tap == PendingTap.LEFT) w * LEFT_EDGE_RATIO else w * RIGHT_EDGE_RATIO
        val y = h * 0.5f
        dispatchTapInternal(x, y)
    }

    private fun dispatchTapInternal(x: Float, y: Float) {
        val path = Path().apply { moveTo(x, y) }
        val gesture = GestureDescription.Builder().addStroke(
            GestureDescription.StrokeDescription(path, 0, 2) // 1ms instead of 10ms
        ).build()
        dispatchGesture(gesture, gestureCallback, null)
    }

    private fun isGamepadSource(source: Int): Boolean {
        return (source and InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD ||
                (source and InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK
    }

    companion object {
        private const val TAG = "PageTurner"
        private const val LEFT_EDGE_RATIO = 0.05f
        private const val RIGHT_EDGE_RATIO = 0.95f

        val SERVICE_CLASS_NAME: String = PageTurnerService::class.java.name
    }
}