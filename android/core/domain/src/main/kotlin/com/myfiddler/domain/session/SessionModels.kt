package com.flowtrace.domain.session

import com.flowtrace.domain.model.AppId
import com.flowtrace.domain.model.SessionId

/**
 * SessionSummary is optimized for list rendering and indexing.
 */
data class SessionSummary(
  val id: SessionId,
  val appId: AppId?,
  val processName: String?,
  val host: String?,
  val scheme: String?,
  val method: String?,
  val path: String?,
  val statusCode: Int?,
  val durationMs: Long?,
  val sentBytes: Long?,
  val receivedBytes: Long?,
  val tlsDecrypted: Boolean,
  val pinningSuspected: Boolean,
  val ruleHit: RuleHit?,
  val createdAtMs: Long,
)

data class SessionDetail(
  val summary: SessionSummary,
  val request: HttpMessage?,
  val response: HttpMessage?,
  val websocketMessages: List<WsMessage>,
  val notes: Map<String, String>,
)

data class HttpMessage(
  val protocol: String?,
  val url: String?,
  val headers: Map<String, String>,
  val body: BodyRef?,
)

data class WsMessage(
  val timestampMs: Long,
  val direction: WsDirection,
  val messageType: Int?,
  val body: BodyRef?,
)

enum class WsDirection { CLIENT_TO_SERVER, SERVER_TO_CLIENT }

data class RuleHit(
  val ruleId: String,
  val action: String,
)

/**
 * BodyRef points to an external storage location (file/db blob).
 * Complex behaviors (truncation, preview) are handled outside domain.
 */
data class BodyRef(
  val storageKey: String,
  val sizeBytes: Long,
  val truncated: Boolean,
  val previewUtf8: String?,
)

