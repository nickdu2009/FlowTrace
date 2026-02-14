package com.flowtrace.capture

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.VpnService
import android.os.Build
import android.os.IBinder
import android.os.ParcelFileDescriptor
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

  private var tunFd: ParcelFileDescriptor? = null

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
    closeTun()
    super.onDestroy()
  }

  override fun onRevoke() {
    Timber.w("VPN permission revoked by system")
    runtime.stop()
    closeTun()
    stopForeground(STOP_FOREGROUND_REMOVE)
    stopSelf()
    super.onRevoke()
  }

  private fun startCapture() {
    Timber.i("Start capture requested")
    startForeground(Notifications.ID, Notifications.build(this, runtime.state.value))
    establishTunIfNeeded()
    runtime.start()
  }

  private fun stopCapture() {
    Timber.i("Stop capture requested")
    runtime.stop()
    closeTun()
    stopForeground(STOP_FOREGROUND_REMOVE)
    stopSelf()
  }

  /**
   * MVP-3: establish a minimal TUN interface.
   *
   * IMPORTANT:
   * - We do NOT forward packets yet (SunnyNet not wired), so we must avoid
   *   routing all device traffic into the VPN.
   * - For safety, we default to only capturing this app's traffic.
   */
  private fun establishTunIfNeeded() {
    if (tunFd != null) return

    val builder = Builder()
      .setSession("FlowTrace Capture")
      .setMtu(1500)
      .addAddress("10.0.0.2", 32)
      .addRoute("0.0.0.0", 0)
      .addDnsServer("8.8.8.8")
      .setConfigureIntent(
        PendingIntent.getActivity(
          this,
          0,
          Intent(this, MainActivity::class.java),
          PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
      )

    // Safety: allow only this app by default to avoid breaking network for other apps.
    runCatching { builder.addAllowedApplication(packageName) }
      .onFailure { Timber.w(it, "Failed to addAllowedApplication(%s)", packageName) }

    // Prefer using the current active network as underlying, when available.
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
      val n: Network? = cm.activeNetwork
      if (n != null) builder.setUnderlyingNetworks(arrayOf(n))
    }

    tunFd = builder.establish()
    if (tunFd == null) {
      Timber.e("Failed to establish TUN interface")
    } else {
      Timber.i("TUN established: fd=%d", tunFd!!.fd)
    }
  }

  private fun closeTun() {
    tunFd?.let {
      runCatching { it.close() }
    }
    tunFd = null
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

