package com.flowtrace.capture

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.flowtrace.MainActivity
import com.flowtrace.R
import com.flowtrace.domain.capture.CaptureState
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import timber.log.Timber

/**
 * MVP-0: VpnService skeleton.
 *
 * - Handles start/stop intents
 * - Runs as a foreground service
 * - Delegates actual engine + pipeline to CaptureRuntime
 *
 * NOTE: This service currently does NOT establish a real TUN interface yet.
 * That will be implemented when SunnyNet JNI bridge is wired (milestone m3/m4).
 */
@AndroidEntryPoint
class VpnCaptureService : VpnService() {

  @Inject lateinit var runtime: CaptureRuntime

  override fun onBind(intent: Intent?): IBinder? = null

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    when (intent?.action) {
      Actions.START_CAPTURE -> startCapture()
      Actions.STOP_CAPTURE -> stopCapture()
      else -> Timber.w("VpnCaptureService unknown action: %s", intent?.action)
    }
    return START_STICKY
  }

  override fun onDestroy() {
    runCatching { runtime.stop() }
    super.onDestroy()
  }

  private fun startCapture() {
    Timber.i("Start capture requested")
    startForeground(Notifications.ID, Notifications.build(this, runtime.state.value))
    runtime.start()
  }

  private fun stopCapture() {
    Timber.i("Stop capture requested")
    runtime.stop()
    stopForeground(STOP_FOREGROUND_REMOVE)
    stopSelf()
  }

  object Actions {
    const val START_CAPTURE = "com.flowtrace.action.START_CAPTURE"
    const val STOP_CAPTURE = "com.flowtrace.action.STOP_CAPTURE"
  }

  object Notifications {
    const val CHANNEL_ID = "flowtrace_capture"
    const val ID = 1001

    fun build(context: Context, state: CaptureState): Notification {
      ensureChannel(context)

      val openAppIntent = Intent(context, MainActivity::class.java)
      val openAppPi = PendingIntent.getActivity(
        context,
        0,
        openAppIntent,
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
      )

      val stopIntent = Intent(context, VpnCaptureService::class.java).apply { action = Actions.STOP_CAPTURE }
      val stopPi = PendingIntent.getService(
        context,
        1,
        stopIntent,
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
      )

      val title = context.getString(R.string.app_name)
      val text = when (state) {
        CaptureState.IDLE -> "Idle"
        CaptureState.STARTING -> "Starting…"
        CaptureState.RUNNING -> "Capturing…"
        CaptureState.STOPPING -> "Stopping…"
        CaptureState.ERROR -> "Error"
      }

      return NotificationCompat.Builder(context, CHANNEL_ID)
        .setSmallIcon(android.R.drawable.stat_sys_download_done)
        .setContentTitle(title)
        .setContentText(text)
        .setContentIntent(openAppPi)
        .setOngoing(true)
        .addAction(0, "Stop", stopPi)
        .build()
    }

    private fun ensureChannel(context: Context) {
      if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
      val mgr = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
      val existing = mgr.getNotificationChannel(CHANNEL_ID)
      if (existing != null) return
      mgr.createNotificationChannel(
        NotificationChannel(
          CHANNEL_ID,
          "FlowTrace Capture",
          NotificationManager.IMPORTANCE_LOW
        ).apply {
          description = "Foreground notification for capture service"
        }
      )
    }
  }
}

