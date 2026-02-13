package com.flowtrace.domain.capture

import com.flowtrace.domain.model.AppId
import com.flowtrace.domain.model.SessionId
import com.flowtrace.domain.session.BodyRef

sealed interface CaptureEvent {
  val sessionId: SessionId
  val timestampMs: Long
  val appId: AppId?
  val processName: String?
}

sealed interface HttpEvent : CaptureEvent {
  val method: String?
  val url: String?
  val host: String?
}

data class HttpRequestStarted(
  override val sessionId: SessionId,
  override val timestampMs: Long,
  override val appId: AppId?,
  override val processName: String?,
  override val method: String?,
  override val url: String?,
  override val host: String?,
  val protocol: String?,
  val headers: Map<String, String>,
  val body: BodyRef?,
) : HttpEvent

data class HttpResponseCompleted(
  override val sessionId: SessionId,
  override val timestampMs: Long,
  override val appId: AppId?,
  override val processName: String?,
  override val method: String?,
  override val url: String?,
  override val host: String?,
  val protocol: String?,
  val statusCode: Int?,
  val statusText: String?,
  val headers: Map<String, String>,
  val body: BodyRef?,
  val serverAddress: String?,
  val tlsDecrypted: Boolean,
) : HttpEvent

data class HttpRequestFailed(
  override val sessionId: SessionId,
  override val timestampMs: Long,
  override val appId: AppId?,
  override val processName: String?,
  override val method: String?,
  override val url: String?,
  override val host: String?,
  val errorMessage: String,
) : HttpEvent

sealed interface WsEvent : CaptureEvent {
  val url: String?
}

data class WsConnected(
  override val sessionId: SessionId,
  override val timestampMs: Long,
  override val appId: AppId?,
  override val processName: String?,
  override val url: String?,
) : WsEvent

data class WsMessageFrame(
  override val sessionId: SessionId,
  override val timestampMs: Long,
  override val appId: AppId?,
  override val processName: String?,
  override val url: String?,
  val direction: WsDirection,
  val messageType: Int?,
  val body: BodyRef?,
) : WsEvent

data class WsDisconnected(
  override val sessionId: SessionId,
  override val timestampMs: Long,
  override val appId: AppId?,
  override val processName: String?,
  override val url: String?,
  val reason: String?,
) : WsEvent

enum class WsDirection { CLIENT_TO_SERVER, SERVER_TO_CLIENT }

