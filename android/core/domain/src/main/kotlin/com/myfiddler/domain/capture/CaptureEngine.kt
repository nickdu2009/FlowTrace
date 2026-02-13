package com.flowtrace.domain.capture

import com.flowtrace.domain.session.SessionId
import kotlinx.coroutines.flow.Flow

/**
 * CaptureEngine is a port that abstracts VPN/TUN + native capture implementation.
 */
interface CaptureEngine {
  val events: Flow<CaptureEvent>

  suspend fun start(config: CaptureConfig): Result<Unit>
  suspend fun stop(): Result<Unit>

  /**
   * Apply a decision back to the underlying engine (best-effort).
   */
  suspend fun applyDecision(decision: CaptureDecision): Result<Unit>
}

data class CaptureConfig(
  val allowedApps: Set<String> = emptySet(),
  val disallowedApps: Set<String> = emptySet(),
  val enableTlsMitm: Boolean = true,
  val dnsMode: DnsMode = DnsMode.LOCAL,
)

enum class DnsMode { LOCAL, REMOTE, REMOTE_DOH_DOT }

sealed interface CaptureDecision {
  val sessionId: SessionId

  data class Allow(override val sessionId: SessionId) : CaptureDecision
  data class Block(override val sessionId: SessionId) : CaptureDecision
  data class RewriteHeaders(
    override val sessionId: SessionId,
    val set: Map<String, String>,
    val remove: Set<String>,
  ) : CaptureDecision

  data class HostMap(
    override val sessionId: SessionId,
    val ip: String,
    val port: Int,
  ) : CaptureDecision

  data class ReplaceBody(
    override val sessionId: SessionId,
    val bodyUtf8: String,
  ) : CaptureDecision
}

