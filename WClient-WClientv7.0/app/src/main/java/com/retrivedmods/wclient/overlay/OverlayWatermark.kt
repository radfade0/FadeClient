package com.retrivedmods.wclient.overlay

import android.app.Service
import android.graphics.PixelFormat
import android.hardware.input.InputManager
import android.os.Build
import android.view.Gravity
import android.view.WindowManager.LayoutParams
import androidx.compose.runtime.Composable

class WatermarkOverlay : OverlayWindow() {

    override val layoutParams by lazy {
        LayoutParams().apply {
            width = LayoutParams.MATCH_PARENT
            height = LayoutParams.MATCH_PARENT
            gravity = Gravity.TOP or Gravity.START
            type = LayoutParams.TYPE_APPLICATION_OVERLAY
            format = PixelFormat.TRANSLUCENT
            flags = LayoutParams.FLAG_NOT_FOCUSABLE or
                    LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    LayoutParams.FLAG_NOT_TOUCHABLE // <-- Important: lets touches pass through
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                alpha = (OverlayManager.currentContext!!
                    .getSystemService(Service.INPUT_SERVICE) as? InputManager)
                    ?.maximumObscuringOpacityForTouch ?: 0.8f
            }
        }
    }

    @Composable
    override fun Content() {
        Watermark()
    }
}
