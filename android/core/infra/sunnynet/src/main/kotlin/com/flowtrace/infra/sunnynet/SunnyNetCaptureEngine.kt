package com.flowtrace.infra.sunnynet

import com.flowtrace.domain.capture.CaptureConfig
import com.flowtrace.domain.capture.CaptureDecision
import com.flowtrace.domain.capture.CaptureEngine
import com.flowtrace.domain.capture.CaptureEvent
import com.flowtrace.domain.capture.CaptureState
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber

/**
 * SunnyNet-backed CaptureEngine (M4 skeleton).
 *
 * Current behavior:
 * - Tries to call into SunnyNet via [SunnyNetBridge]
 * - If native library is missing or calls fail, falls back to [FakeSunnyNetCaptureEngine]
 *
 * TODO(M4/M5):
 * - Wire JNI callbacks into [events]
 * - Apply decisions back to SunnyNet for rules (block/rewrite/hostmap)
 */
@Singleton
class SunnyNetCaptureEngine @Inject constructor(
  private val bridge: SunnyNetJniBridge,
  private val fallback: FakeSunnyNetCaptureEngine,
) : CaptureEngine {

  private val _state = MutableStateFlow(CaptureState.IDLE)
  override val state = _state.asStateFlow()

  private val _events = MutableSharedFlow<CaptureEvent>(extraBufferCapacity = 512)
  override val events: Flow<CaptureEvent> = _events.asSharedFlow()

  private var ctx: Long = 0L
  private var usingFallback: Boolean = false

  override suspend fun start(config: CaptureConfig): Result<Unit> {
    if (_state.value == CaptureState.RUNNING || _state.value == CaptureState.STARTING) return Result.success(Unit)
    _state.value = CaptureState.STARTING

    // Try SunnyNet native
    val ok = runCatching {
      ctx = bridge.createContext()
      if (ctx == 0L) return@runCatching false

      // devMode=2 => Tun (Android VPN)
      if (!bridge.openDrive(ctx, 2)) return@runCatching false

      // Per-app capture (best-effort)
      if (config.allowedApps.isNotEmpty()) {
        bridge.processAll(ctx, enable = false, stopNetwork = false)
        config.allowedApps.forEach { bridge.processAddName(ctx, it) }
      } else if (config.disallowedApps.isNotEmpty()) {
        bridge.processAll(ctx, enable = true, stopNetwork = false)
        config.disallowedApps.forEach { bridge.processDelName(ctx, it) }
      } else {
        // Default: capture all (SunnyNet side). VPN side is still allow-listed to this app in M3.
        bridge.processAll(ctx, enable = true, stopNetwork = false)
      }

      bridge.start(ctx)
    }.getOrDefault(false)

    if (!ok) {
      usingFallback = true
      Timber.w("SunnyNet start failed (%s); using Fake engine", bridge.error(ctx))
      _state.value = CaptureState.RUNNING
      // Mirror fallback state/events
      return fallback.start(config)
    }

    usingFallback = false
    _state.value = CaptureState.RUNNING
    // NOTE: events are not yet wired until JNI callbacks are implemented.
    return Result.success(Unit)
  }

  override suspend fun stop(): Result<Unit> {
    if (_state.value == CaptureState.IDLE) return Result.success(Unit)
    _state.value = CaptureState.STOPPING

    if (usingFallback) {
      val r = fallback.stop()
      _state.value = CaptureState.IDLE
      return r
    }

    runCatching {
      if (ctx != 0L) {
        bridge.stop(ctx)
        bridge.unDrive(ctx)
        bridge.releaseContext(ctx)
      }
    }.onFailure {
      Timber.w(it, "SunnyNet stop failed")
    }

    ctx = 0L
    _state.value = CaptureState.IDLE
    return Result.success(Unit)
  }

  override suspend fun applyDecision(decision: CaptureDecision): Result<Unit> {
    if (usingFallback) return fallback.applyDecision(decision)
    // TODO: map decision to SunnyNet action APIs once JNI signatures are confirmed.
    return Result.success(Unit)
  }
}

