package com.innojane.joyconinkturner.ui.widgets

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.withStyledAttributes
import com.innojane.joyconinkturner.R

class ToggleRowView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val labelView = TextView(context)
    private val iconView = ImageView(context)

    var text: CharSequence
        get() = labelView.text
        set(value) {
            labelView.text = value
        }

    var isOn: Boolean
        get() = iconView.isSelected
        set(value) {
            iconView.isSelected = value
        }

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        isClickable = true
        isFocusable = true
        minimumHeight = dp(48)
        setPadding(dp(0), dp(8), dp(0), dp(8))

        val iconSize = dp(40)
        val iconHeight = dp(20)

        labelView.text = "Toggle"
        labelView.layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT).apply {
            weight = 1f
        }

        iconView.layoutParams = LayoutParams(iconSize, iconHeight).apply {
            marginStart = dp(12)
        }
        iconView.scaleType = ImageView.ScaleType.FIT_CENTER
        iconView.setImageResource(R.drawable.switch_icon_selector)

        addView(labelView)
        addView(iconView)

        context.withStyledAttributes(attrs, R.styleable.ToggleRowView) {
            labelView.text = getString(R.styleable.ToggleRowView_toggleText) ?: labelView.text
            iconView.setImageResource(
                getResourceId(R.styleable.ToggleRowView_toggleIcon, R.drawable.switch_icon_selector)
            )
        }
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()
}