package com.flowtrace.infra.sunnynet

import com.SunnyNet.Internal.HTTPEvent
import com.SunnyNet.Internal.TCPEvent
import com.SunnyNet.Internal.UDPEvent
import com.SunnyNet.Internal.WebSocketEvent

internal sealed interface SunnyNetNativeEvent {
  val timestampMs: Long
}

internal data class SunnyNetHttpNativeEvent(
  override val timestampMs: Long,
  val theology: Long,
  val messageId: Long,
  val type: Long,
  val method: String?,
  val url: String?,
  val error: String?,
  val pid: Long,
) : SunnyNetNativeEvent

internal data class SunnyNetWsNativeEvent(
  override val timestampMs: Long,
  val theology: Long,
  val messageId: Long,
  val type: Long,
  val method: String?,
  val url: String?,
  val pid: Long,
  val messageType: Long,
) : SunnyNetNativeEvent

internal data class SunnyNetTcpNativeEvent(
  override val timestampMs: Long,
  val theology: Long,
  val messageId: Long,
  val type: Long,
  val localAddress: String?,
  val remoteAddress: String?,
  val pid: Long,
  val data: ByteArray?,
) : SunnyNetNativeEvent

internal data class SunnyNetUdpNativeEvent(
  override val timestampMs: Long,
  val theology: Long,
  val messageId: Long,
  val type: Long,
  val localAddress: String?,
  val remoteAddress: String?,
  val pid: Long,
) : SunnyNetNativeEvent

/**
 * Object passed to SunnyNet via JNI for receiving callbacks.
 *
 * Method names & signatures MUST match SunnyNet's hardcoded GetMethodID lookups:
 * - onHTTPCallback(HTTPEvent)
 * - onWebSocketCallback(WebSocketEvent)
 * - onTCPCallback(TCPEvent)
 * - onUDPCallback(UDPEvent)
 * - onScriptLogCallback(long, String)
 * - onScriptCodeSaveCallback(long, String)
 */
@Suppress("unused")
internal class SunnyNetCallbackReceiver(
  private val sink: (SunnyNetNativeEvent) -> Unit,
) {
  fun onHTTPCallback(e: HTTPEvent) {
    sink(
      SunnyNetHttpNativeEvent(
        timestampMs = System.currentTimeMillis(),
        theology = e.theology,
        messageId = e.messageId,
        type = e.type,
        method = e.method,
        url = e.url,
        error = e.error,
        pid = e.pid,
      )
    )
  }

  fun onWebSocketCallback(e: WebSocketEvent) {
    sink(
      SunnyNetWsNativeEvent(
        timestampMs = System.currentTimeMillis(),
        theology = e.theology,
        messageId = e.messageId,
        type = e.type,
        method = e.method,
        url = e.url,
        pid = e.pid,
        messageType = e.messageType,
      )
    )
  }

  fun onTCPCallback(e: TCPEvent) {
    sink(
      SunnyNetTcpNativeEvent(
        timestampMs = System.currentTimeMillis(),
        theology = e.theology,
        messageId = e.messageId,
        type = e.type,
        localAddress = e.localAddress,
        remoteAddress = e.remoteAddress,
        pid = e.pid,
        data = e.data,
      )
    )
  }

  fun onUDPCallback(e: UDPEvent) {
    sink(
      SunnyNetUdpNativeEvent(
        timestampMs = System.currentTimeMillis(),
        theology = e.theology,
        messageId = e.messageId,
        type = e.type,
        localAddress = e.localAddress,
        remoteAddress = e.remoteAddress,
        pid = e.pid,
      )
    )
  }

  fun onScriptLogCallback(context: Long, info: String?) = Unit
  fun onScriptCodeSaveCallback(context: Long, code: String?) = Unit
}

