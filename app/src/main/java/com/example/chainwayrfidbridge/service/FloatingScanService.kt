package com.example.chainwayrfidbridge.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.WindowManager
import android.widget.TextView
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.chainwayrfidbridge.MainActivity
import com.example.chainwayrfidbridge.R
import com.example.chainwayrfidbridge.data.ConfigRepository
import com.example.chainwayrfidbridge.ui.stringsFor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlin.math.abs

private const val CHANNEL_ID = "floating_scan_status"
private const val NOTIFICATION_ID = 1001
private const val BUBBLE_SIZE_DP = 44

private const val COLOR_IDLE = 0xFF9E9E9E.toInt()
private const val COLOR_SCANNING = 0xFF1565C0.toInt()
private const val COLOR_SUCCESS = 0xFF2E7D32.toInt()
private const val COLOR_ERROR = 0xFFC62828.toInt()

/**
 * Small always-on-top status dot shown while the app is backgrounded with background scanning
 * enabled — lets the operator glance at scan state (idle/scanning/sent/failed) while using
 * another app (e.g. a browser) instead of switching back and forth. Tapping it reopens the app.
 * All reader/network work still happens only in ScanViewModel; this just mirrors its state via
 * ScanStateBus and never touches the reader itself.
 */
class FloatingScanService : Service() {

    private var windowManager: WindowManager? = null
    private var bubbleView: TextView? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun onCreate() {
        super.onCreate()
        // Must always succeed: if this service was started via startForegroundService() and
        // onCreate() throws before startForeground() actually runs, Android kills the whole app
        // process (not just this service) with a fatal RemoteServiceException — so the "nice"
        // notification build is wrapped and never allowed to prevent startForeground() from
        // being called at all.
        startForeground(NOTIFICATION_ID, buildNotificationSafely())
        if (!Settings.canDrawOverlays(this)) {
            stopSelf()
            return
        }
        runCatching { addBubble() }
        scope.launch {
            ScanStateBus.data.collect { updateBubble(it) }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun addBubble() {
        val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        windowManager = wm

        val sizePx = (BUBBLE_SIZE_DP * resources.displayMetrics.density).toInt()
        val view = TextView(this).apply {
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(COLOR_IDLE)
            }
            setTextColor(Color.WHITE)
            textSize = 11f
            gravity = Gravity.CENTER
            includeFontPadding = false
            maxLines = 1
        }

        val overlayType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }
        val params = WindowManager.LayoutParams(
            sizePx,
            sizePx,
            overlayType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 0
            y = 200
        }

        // Simple drag-to-move + tap-to-open, distinguished by total movement since ACTION_DOWN.
        var downX = 0f
        var downY = 0f
        var startX = 0
        var startY = 0
        var dragged = false
        view.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    downX = event.rawX
                    downY = event.rawY
                    startX = params.x
                    startY = params.y
                    dragged = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - downX).toInt()
                    val dy = (event.rawY - downY).toInt()
                    if (abs(dx) > 12 || abs(dy) > 12) dragged = true
                    params.x = startX + dx
                    params.y = startY + dy
                    runCatching { wm.updateViewLayout(view, params) }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!dragged) openApp()
                    true
                }
                else -> false
            }
        }

        runCatching { wm.addView(view, params) }
        bubbleView = view
    }

    private fun openApp() {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        }
        startActivity(intent)
    }

    private fun updateBubble(data: ScanStateBus.BubbleData) {
        val color = when (data.state) {
            ScanStateBus.BubbleState.IDLE -> COLOR_IDLE
            ScanStateBus.BubbleState.SCANNING -> COLOR_SCANNING
            ScanStateBus.BubbleState.SUCCESS -> COLOR_SUCCESS
            ScanStateBus.BubbleState.ERROR -> COLOR_ERROR
        }
        val view = bubbleView ?: return
        (view.background as? GradientDrawable)?.setColor(color)
        view.text = if (data.state == ScanStateBus.BubbleState.IDLE) "" else data.tagCount.toString()
    }

    private fun buildNotificationSafely(): Notification {
        ensureChannel()
        return try {
            buildNotification()
        } catch (e: Exception) {
            // Bare-bones fallback with zero external dependencies (no ConfigRepository, no
            // strings, no Activity intent) — guaranteed not to throw, so startForeground() always
            // has something valid to call with.
            NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Stechoq RFID Suite")
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .setOngoing(true)
                .build()
        }
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        try {
            val manager = getSystemService(NotificationManager::class.java) ?: return
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "RFID background scan", NotificationManager.IMPORTANCE_MIN)
            )
        } catch (e: Exception) {
            // best-effort — if this fails, NotificationCompat.Builder below still works on API 26+
            // as long as SOME channel with this ID exists; if none does the notification is just dropped
        }
    }

    private fun buildNotification(): Notification {
        val strings = stringsFor(ConfigRepository(this).loadLanguage())
        val openIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT),
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(strings.appTitle)
            .setContentText(strings.backgroundScanActive)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setOngoing(true)
            .setContentIntent(openIntent)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
        bubbleView?.let { view -> runCatching { windowManager?.removeView(view) } }
        bubbleView = null
    }

    companion object {
        fun start(context: Context) {
            if (!Settings.canDrawOverlays(context)) return
            ContextCompat.startForegroundService(context, Intent(context, FloatingScanService::class.java))
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, FloatingScanService::class.java))
        }
    }
}
