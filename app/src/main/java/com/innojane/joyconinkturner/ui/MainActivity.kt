package com.innojane.joyconinkturner.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.InputDevice
import android.view.InputEvent
import android.view.KeyEvent
import android.view.View
import android.widget.TextView
import androidx.core.content.edit
import com.innojane.joyconinkturner.R
import com.innojane.joyconinkturner.config.Prefs
import com.innojane.joyconinkturner.config.Prefs.KEY_ENABLED
import com.innojane.joyconinkturner.service.PageTurnerService.Companion.SERVICE_CLASS_NAME
import com.innojane.joyconinkturner.ui.widgets.ToggleRowView

class MainActivity : Activity() {
    private var accessibilityDialogShown = false
    private lateinit var accessibilityToggle: ToggleRowView
    private lateinit var pageTurnerToggle: ToggleRowView
    private lateinit var gamepadText: TextView
    private lateinit var customizedToggle: ToggleRowView
    private var customizedEnabled = false


    private val prefs by lazy {
        applicationContext.getSharedPreferences(Prefs.FILE_NAME, MODE_PRIVATE)
    }

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        gamepadText = findViewById(R.id.gamepadDevicesText)

        accessibilityToggle = findViewById(R.id.accessibilityToggle)
        accessibilityToggle.setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }

        pageTurnerToggle = findViewById(R.id.pageTurnerToggle)
        pageTurnerToggle.setOnClickListener {
            if (!isMyAccessibilityServiceEnabled(this)) return@setOnClickListener
            val enabled = !prefs.getBoolean(KEY_ENABLED, true)
            prefs.edit { putBoolean(KEY_ENABLED, enabled) }
            renderPageTurnerToggle()
        }

        customizedToggle = findViewById(R.id.customizedToggle)
        customizedToggle.isOn = customizedEnabled
        customizedToggle.setOnClickListener {
            customizedEnabled = !customizedEnabled
            customizedToggle.isOn = customizedEnabled
        }

        refreshGamepadList()
        renderPageTurnerToggle()
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {

        if (event.action == KeyEvent.ACTION_DOWN || event.action == KeyEvent.ACTION_UP) {
            Log.d(
                "KeyProbe",
                "act=${if (event.action == 0) "DOWN" else "UP"} keyCode=${event.keyCode} " +
                        "source=${event.source} repeat=${event.repeatCount}"
            )
        }

        if (event.action != KeyEvent.ACTION_DOWN) return super.dispatchKeyEvent(event)

        val sourceHex = "0x" + event.source.toString(16)

        val dev: InputDevice? = InputDevice.getDevice(event.deviceId)

        val isGamepadEvent = isGamepadEvent(event)

        if (!isGamepadEvent) return super.dispatchKeyEvent(event)

        Log.d(
            TAG,
            "source=${event.source} keyCode=${event.keyCode} source=$sourceHex " +
                    "isGamepadEvent=$isGamepadEvent deviceId=${event.deviceId} deviceName=${dev?.name} " +
                    "vendorId=${dev?.vendorId} productId=${dev?.productId}"
        )

        return true
    }

    override fun onResume() {
        super.onResume()
        renderAccessibilityToggle()
        renderPageTurnerToggle()
        refreshGamepadList()
        maybeShowAccessibilityDialog()
    }

    private fun isGamepadSource(source: Int): Boolean {
        return (source and InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD ||
                (source and InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK
    }

    private fun isGamepadEvent(event: InputEvent): Boolean {
        return isGamepadSource(event.source)
    }

    private fun refreshGamepadList() {
        val names = InputDevice.getDeviceIds()
            .asList()
            .mapNotNull { id -> InputDevice.getDevice(id) }
            .filter { isGamepadSource(it.sources) }
            .map { it.name }
            .distinct()

        gamepadText.text = if (names.isEmpty()) {
            "Gamepads:\n(none)"
        } else {
            "Gamepads:\n" + names.joinToString("\n")
        }
    }

    private fun isMyAccessibilityServiceEnabled(context: Context): Boolean {
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false

        val myService = "${context.packageName}/${SERVICE_CLASS_NAME}"
        return enabledServices.contains(myService, ignoreCase = true)
    }

    private fun maybeShowAccessibilityDialog() {
        if (accessibilityDialogShown) return
        if (isMyAccessibilityServiceEnabled(this)) return
        accessibilityDialogShown = true

        val dialog = android.app.AlertDialog.Builder(this)
            .setTitle(getString(R.string.accessibility_dialog_title))
            .setMessage(getString(R.string.accessibility_dialog_message))
            .setPositiveButton(getString(R.string.accessibility_dialog_positive)) { _, _ ->
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }
            .setNegativeButton(getString(R.string.accessibility_dialog_negative)) { _, _ ->
                accessibilityDialogShown = false
            }
            .setOnDismissListener { accessibilityDialogShown = false }
            .show()

        dialog.window?.setBackgroundDrawableResource(R.drawable.dialog_stroke_bg)
    }

    private fun renderAccessibilityToggle() {
        accessibilityToggle.isOn = isMyAccessibilityServiceEnabled(this)
    }

    private fun renderPageTurnerToggle() {
        val accessibilityOn = isMyAccessibilityServiceEnabled(this)
        val enabled = accessibilityOn && prefs.getBoolean(KEY_ENABLED, true)

        pageTurnerToggle.visibility = if (accessibilityOn) View.VISIBLE else View.GONE
        pageTurnerToggle.isOn = enabled

        customizedToggle.visibility = if (enabled) View.VISIBLE else View.GONE
        customizedToggle.isEnabled = enabled
        if (!enabled) customizedToggle.isOn = false

        if (!accessibilityOn) {
            prefs.edit { putBoolean(Prefs.KEY_ENABLED, false) }
        }
    }

    companion object {
        private const val TAG = "JoyConProbe"
    }
}