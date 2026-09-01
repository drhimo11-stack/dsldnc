package com.shield.app.accessibility

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.CountDownTimer
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import com.shield.app.R

/**
 * Draws the block screen as a real system overlay window
 * (TYPE_APPLICATION_OVERLAY) instead of launching it as a normal Activity.
 *
 * Why this exists: launching [BlockOverlayActivity] as a regular Activity
 * only guarantees it's on top of the *task stack* for its own window. On
 * Samsung devices, the Edge panel's "Apps edge" lets a browser be popped
 * out into a small floating mini-window (multi-window/pop-up view). That
 * floating window sits in its own always-on-top layer, so a same-layer
 * Activity started from the Accessibility Service ends up rendering
 * *underneath* it — the overlay is technically showing, just hidden behind
 * the still-interactive mini browser window.
 *
 * A TYPE_APPLICATION_OVERLAY window lives in the system overlay layer,
 * which sits above ordinary app windows (including freeform/pop-up/PIP
 * windows), the same layer used by "draw over other apps" tools like
 * screen-time limiters and call-blockers. That's the only way to guarantee
 * the block screen is actually seen no matter what windowing mode the
 * blocked app is using. Requires the SYSTEM_ALERT_WINDOW ("draw over
 * other apps") permission, which must be granted via [Settings.canDrawOverlays].
 */
class BlockOverlayWindow(private val service: ShieldAccessibilityService) {

    private val windowManager: WindowManager =
        service.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var overlayView: View? = null
    private var countDownTimer: CountDownTimer? = null

    fun isShowing(): Boolean = overlayView != null

    fun show(message: String, countdownSeconds: Int, redirectUrl: String) {
        if (overlayView != null) return // already showing, don't stack a second one
        if (!Settings.canDrawOverlays(service)) return

        val inflater = LayoutInflater.from(service)
        val view = inflater.inflate(R.layout.block_overlay, null)

        val messageView = view.findViewById<TextView>(R.id.overlay_message)
        val closeButton = view.findViewById<Button>(R.id.overlay_close_button)
        messageView.text = message

        if (countdownSeconds <= 0) {
            closeButton.isEnabled = true
            closeButton.text = service.getString(R.string.overlay_close)
        } else {
            closeButton.isEnabled = false
            closeButton.text = service.getString(
                R.string.overlay_close_countdown,
                countdownSeconds
            )
            countDownTimer = object : CountDownTimer(countdownSeconds * 1000L, 1000L) {
                override fun onTick(millisUntilFinished: Long) {
                    val secondsLeft = ((millisUntilFinished / 1000L) + 1).toInt()
                    closeButton.text = service.getString(
                        R.string.overlay_close_countdown,
                        secondsLeft
                    )
                }

                override fun onFinish() {
                    closeButton.isEnabled = true
                    closeButton.text = service.getString(R.string.overlay_close)
                }
            }.start()
        }

        closeButton.setOnClickListener {
            handleClose(redirectUrl)
        }

        val overlayType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            overlayType,
            // Deliberately NOT FLAG_NOT_FOCUSABLE / NOT_TOUCHABLE: the
            // overlay must intercept all touches and take focus so the
            // mini-window/app underneath can't be tapped through it, and
            // so the hardware/gesture back action doesn't fall through to
            // the app behind it either.
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
            android.graphics.PixelFormat.TRANSLUCENT
        )
        // Covers the status bar/cutout area too, matching the old
        // Activity's showWhenLocked/turnScreenOn/fullscreen behavior.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            params.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }

        try {
            windowManager.addView(view, params)
            overlayView = view
        } catch (e: Exception) {
            // Overlay permission revoked between the check above and now,
            // or the window token became invalid — fail closed but don't
            // crash the accessibility service over it.
            overlayView = null
        }
    }

    fun hide() {
        countDownTimer?.cancel()
        countDownTimer = null
        val view = overlayView ?: return
        overlayView = null
        try {
            windowManager.removeView(view)
        } catch (e: Exception) {
            // View already detached — nothing to clean up.
        }
    }

    private fun handleClose(redirectUrl: String) {
        val target = normalizeRedirectUrl(redirectUrl)
        try {
            if (target != null) {
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(target)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                service.startActivity(browserIntent)
            } else {
                goHome()
            }
        } catch (e: Exception) {
            goHome()
        }
        hide()
    }

    private fun goHome() {
        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        service.startActivity(homeIntent)
    }

    private fun normalizeRedirectUrl(url: String): String? {
        val trimmed = url.trim()
        if (trimmed.isEmpty()) return null
        return if (trimmed.contains("://")) trimmed else "https://$trimmed"
    }
}
