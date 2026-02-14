package com.flowtrace.infra.sunnynet

import com.flowtrace.domain.capture.CaptureConfig
import com.flowtrace.domain.capture.CaptureDecision
import com.flowtrace.domain.capture.CaptureEngine
import com.flowtrace.domain.capture.CaptureEvent
import com.flowtrace.domain.capture.CaptureState
import com.flowtrace.domain.capture.HttpRequestFailed
import com.flowtrace.domain.capture.HttpRequestStarted
import com.flowtrace.domain.capture.HttpResponseCompleted
import com.flowtrace.domain.capture.WsConnected
import com.flowtrace.domain.capture.WsDisconnected
import com.flowtrace.domain.capture.WsMessageFrame
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
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
  private val tunFdProvider: TunFdProvider,
) : CaptureEngine {

  private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

  private val _state = MutableStateFlow(CaptureState.IDLE)
  override val state = _state.asStateFlow()

  private val _events = MutableSharedFlow<CaptureEvent>(extraBufferCapacity = 512)
  override val events: Flow<CaptureEvent> = _events.asSharedFlow()

  private val nativeQueue = Channel<SunnyNetNativeEvent>(
    capacity = 4096,
    onBufferOverflow = BufferOverflow.DROP_OLDEST,
  )

  private val callbackReceiver = SunnyNetCallbackReceiver { ev ->
    val r = nativeQueue.trySend(ev)
    if (r.isFailure) {
      // DROP_OLDEST should avoid failure in most cases, but still guard.
      Timber.w("SunnyNet native queue overflow; dropping %s", ev::class.simpleName)
    }
  }

  private var ctx: Long = 0L
  private var usingFallback: Boolean = false
  private var mapperJob: Job? = null
  private var fallbackForwardJob: Job? = null

  override suspend fun start(config: CaptureConfig): Result<Unit> {
    if (_state.value == CaptureState.RUNNING || _state.value == CaptureState.STARTING) return Result.success(Unit)
    _state.value = CaptureState.STARTING

    // Try SunnyNet native
    val ok = runCatching {
      ctx = bridge.createContext()
      if (ctx == 0L) return@runCatching false

      // Must set callback BEFORE starting (SunnyNet uses it for event delivery)
      if (!bridge.setCallback(ctx, callbackReceiver)) return@runCatching false

      // devMode=2 => Tun (Android VPN)
      if (!bridge.openDrive(ctx, 2)) return@runCatching false

      // Pass TUN fd (best-effort). Without this, tun driver cannot read packets.
      tunFdProvider.consumeOnce()?.let { fd ->
        val fdOk = bridge.setTunFd(fd)
        if (!fdOk) Timber.w("SunnyNet setTunFd failed (fd=%d). Capture may not work.", fd)
      }

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
      mapperJob?.cancel()
      mapperJob = null

      fallbackForwardJob?.cancel()
      fallbackForwardJob = scope.launch {
        launch { fallback.state.collect { _state.value = it } }
        launch { fallback.events.collect { _events.tryEmit(it) } }
      }

      return fallback.start(config)
    }

    usingFallback = false
    _state.value = CaptureState.RUNNING

    fallbackForwardJob?.cancel()
    fallbackForwardJob = null

    mapperJob?.cancel()
    mapperJob = scope.launch {
      for (ev in nativeQueue) {
        mapAndEmit(ev)
      }
    }

    return Result.success(Unit)
  }

  override suspend fun stop(): Result<Unit> {
    if (_state.value == CaptureState.IDLE) return Result.success(Unit)
    _state.value = CaptureState.STOPPING

    if (usingFallback) {
      val r = fallback.stop()
      fallbackForwardJob?.cancel()
      fallbackForwardJob = null
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

    mapperJob?.cancel()
    mapperJob = null
    fallbackForwardJob?.cancel()
    fallbackForwardJob = null
    ctx = 0L
    _state.value = CaptureState.IDLE
    return Result.success(Unit)
  }

  override suspend fun applyDecision(decision: CaptureDecision): Result<Unit> {
    if (usingFallback) return fallback.applyDecision(decision)
    // TODO: map decision to SunnyNet action APIs once JNI signatures are confirmed.
    return Result.success(Unit)
  }

  private fun mapAndEmit(ev: SunnyNetNativeEvent) {
    when (ev) {
      is SunnyNetHttpNativeEvent -> mapHttp(ev)?.let { _events.tryEmit(it) }
      is SunnyNetWsNativeEvent -> mapWs(ev)?.let { _events.tryEmit(it) }
      is SunnyNetTcpNativeEvent -> Unit // MVP: ignore TCP raw events for now
      is SunnyNetUdpNativeEvent -> Unit // MVP: ignore UDP raw events for now
    }
  }

  private fun mapHttp(e: SunnyNetHttpNativeEvent): CaptureEvent? {
    val sid = com.flowtrace.domain.model.SessionId("http:${e.messageId}")
    val url = e.url
    val host = url?.substringAfter("://")?.substringBefore("/")

    return when (e.type.toInt()) {
      HttpSendRequest -> {
        val headers = parseHeaders(bridge.getRequestAllHeader(e.messageId))
        HttpRequestStarted(
          sessionId = sid,
          timestampMs = e.timestampMs,
          appId = null,
          processName = null,
          method = e.method,
          url = url,
          host = host,
          protocol = bridge.getRequestProto(e.messageId).ifBlank { null },
          headers = headers,
          body = null,
        )
      }

      HttpResponseOK -> {
        val headers = parseHeaders(bridge.getResponseAllHeader(e.messageId))
        val statusCode = bridge.getResponseStatusCode(e.messageId).takeIf { it >= 0 }
        HttpResponseCompleted(
          sessionId = sid,
          timestampMs = e.timestampMs,
          appId = null,
          processName = null,
          method = e.method,
          url = url,
          host = host,
          protocol = bridge.getResponseProto(e.messageId).ifBlank { null },
          statusCode = statusCode,
          statusText = bridge.getResponseStatus(e.messageId).ifBlank { null },
          headers = headers,
          body = null,
          serverAddress = bridge.getResponseServerAddress(e.messageId).ifBlank { null },
          tlsDecrypted = (url ?: "").startsWith("https://"),
          timing = null,
        )
      }

      HttpRequestFail -> {
        HttpRequestFailed(
          sessionId = sid,
          timestampMs = e.timestampMs,
          appId = null,
          processName = null,
          method = e.method,
          url = url,
          host = host,
          errorMessage = e.error.orEmpty(),
          failureStage = null,
        )
      }

      else -> null
    }
  }

  private fun mapWs(e: SunnyNetWsNativeEvent): CaptureEvent? {
    val sid = com.flowtrace.domain.model.SessionId("ws:${e.theology}")
    val url = e.url
    return when (e.type.toInt()) {
      WebsocketConnectionOK -> WsConnected(
        sessionId = sid,
        timestampMs = e.timestampMs,
        appId = null,
        processName = null,
        url = url,
      )

      WebsocketDisconnect -> WsDisconnected(
        sessionId = sid,
        timestampMs = e.timestampMs,
        appId = null,
        processName = null,
        url = url,
        reason = null,
      )

      WebsocketUserSend -> WsMessageFrame(
        sessionId = sid,
        timestampMs = e.timestampMs,
        appId = null,
        processName = null,
        url = url,
        direction = com.flowtrace.domain.model.WsDirection.CLIENT_TO_SERVER,
        messageType = e.messageType.toInt(),
        body = null,
      )

      WebsocketServerSend -> WsMessageFrame(
        sessionId = sid,
        timestampMs = e.timestampMs,
        appId = null,
        processName = null,
        url = url,
        direction = com.flowtrace.domain.model.WsDirection.SERVER_TO_CLIENT,
        messageType = e.messageType.toInt(),
        body = null,
      )

      else -> null
    }
  }

  private fun parseHeaders(raw: String): List<Pair<String, String>> {
    if (raw.isBlank()) return emptyList()
    val lines = raw
      .replace("\r", "")
      .split("\n")
      .map { it.trimEnd() }
      .filter { it.isNotBlank() }

    val out = ArrayList<Pair<String, String>>(lines.size)
    for (line in lines) {
      val idx = line.indexOf(':')
      if (idx <= 0) continue
      val k = line.substring(0, idx).trim()
      val v = line.substring(idx + 1).trim()
      if (k.isNotEmpty()) out.add(k to v)
    }
    return out
  }

  private companion object {
    // From SunnyNet official constants (src/public/constobj.go)
    const val HttpSendRequest = 1
    const val HttpResponseOK = 2
    const val HttpRequestFail = 3

    const val WebsocketConnectionOK = 1
    const val WebsocketUserSend = 2
    const val WebsocketServerSend = 3
    const val WebsocketDisconnect = 4
  }
}

