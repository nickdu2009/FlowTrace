package com.flowtrace.domain.capture

import com.flowtrace.domain.model.SessionId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * CaptureEngine is a port that abstracts VPN/TUN + native capture implementation.
 */
interface CaptureEngine {
  /** 事件流：native 回调产出的抓包事件 */
  val events: Flow<CaptureEvent>

  /** 抓包引擎的当前状态（UI/ViewModel 订阅） */
  val state: StateFlow<CaptureState>

  suspend fun start(config: CaptureConfig): Result<Unit>
  suspend fun stop(): Result<Unit>

  /**
   * Apply a decision back to the underlying engine (best-effort).
   */
  suspend fun applyDecision(decision: CaptureDecision): Result<Unit>
}

/**
 * VPN 抓包生命周期状态机，与 doc/ARCHITECTURE.md 对齐：
 *   Idle → Starting → Running → Stopping → Idle
 *                     Running → Error → Idle
 */
enum class CaptureState {
  /** 未运行 / 已停止 */
  IDLE,
  /** 正在启动（VPN 授权 + native 引擎初始化） */
  STARTING,
  /** 正常运行中 */
  RUNNING,
  /** 正在停止 */
  STOPPING,
  /** 发生错误（vpnRevoked / nativeCrash / fatalError），需要用户确认后回到 IDLE */
  ERROR,
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
