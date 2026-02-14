package com.flowtrace.infra.sunnynet

import com.flowtrace.domain.capture.CaptureConfig
import com.flowtrace.domain.capture.CaptureDecision
import com.flowtrace.domain.capture.CaptureEngine
import com.flowtrace.domain.capture.CaptureState
import com.flowtrace.domain.capture.HttpRequestStarted
import com.flowtrace.domain.capture.HttpResponseCompleted
import com.flowtrace.domain.capture.Timing
import com.flowtrace.domain.model.AppId
import com.flowtrace.domain.model.SessionId
import com.flowtrace.domain.session.BodyRef
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * MVP-0 Fake engine: emits synthetic HTTP events to validate the pipeline end-to-end.
 *
 * Replace this with real SunnyNet JNI backed engine in milestone m4.
 */
@Singleton
class FakeSunnyNetCaptureEngine @Inject constructor() : CaptureEngine {

  private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

  private val _state = MutableStateFlow(CaptureState.IDLE)
  override val state = _state.asStateFlow()

  private val _events = MutableSharedFlow<com.flowtrace.domain.capture.CaptureEvent>(
    extraBufferCapacity = 256,
  )
  override val events: Flow<com.flowtrace.domain.capture.CaptureEvent> = _events.asSharedFlow()

  private var job: Job? = null

  override suspend fun start(config: CaptureConfig): Result<Unit> {
    if (_state.value == CaptureState.RUNNING || _state.value == CaptureState.STARTING) return Result.success(Unit)
    _state.value = CaptureState.STARTING

    job?.cancel()
    job = scope.launch {
      _state.value = CaptureState.RUNNING
      while (true) {
        emitOneSession()
        delay(250L)
      }
    }
    return Result.success(Unit)
  }

  override suspend fun stop(): Result<Unit> {
    if (_state.value == CaptureState.IDLE) return Result.success(Unit)
    _state.value = CaptureState.STOPPING
    job?.cancel()
    job = null
    _state.value = CaptureState.IDLE
    return Result.success(Unit)
  }

  override suspend fun applyDecision(decision: CaptureDecision): Result<Unit> {
    // Fake engine ignores decisions (best-effort no-op)
    return Result.success(Unit)
  }

  private suspend fun emitOneSession() {
    val sid = SessionId(Random.nextLong().toString())
    val now = System.currentTimeMillis()
    val url = listOf(
      "https://example.com/api/v1/user",
      "https://example.com/api/v1/orders",
      "http://httpbin.org/get",
    ).random()

    val req = HttpRequestStarted(
      sessionId = sid,
      timestampMs = now,
      appId = AppId("com.flowtrace.fake"),
      processName = "fake",
      method = "GET",
      url = url,
      host = url.substringAfter("://").substringBefore("/"),
      protocol = "HTTP/2",
      headers = listOf(
        "User-Agent" to "FlowTrace-Fake/0.1",
        "Accept" to "application/json",
      ),
      body = null,
    )
    _events.tryEmit(req)

    delay(Random.nextLong(20, 180))

    val bodyPreview = """{"ok":true,"id":"${sid.value.takeLast(6)}"}"""
    val resp = HttpResponseCompleted(
      sessionId = sid,
      timestampMs = System.currentTimeMillis(),
      appId = req.appId,
      processName = req.processName,
      method = req.method,
      url = req.url,
      host = req.host,
      protocol = "HTTP/2",
      statusCode = 200,
      statusText = "OK",
      headers = listOf(
        "Content-Type" to "application/json",
        "Set-Cookie" to "sid=****; HttpOnly",
        "Set-Cookie" to "ab=****; HttpOnly",
      ),
      body = BodyRef(
        storageKey = "fake:${sid.value}",
        sizeBytes = bodyPreview.toByteArray().size.toLong(),
        truncated = false,
        previewUtf8 = bodyPreview,
      ),
      serverAddress = "93.184.216.34:443",
      tlsDecrypted = url.startsWith("https://"),
      timing = Timing(
        dnsMs = Random.nextLong(1, 8),
        connectMs = Random.nextLong(5, 20),
        tlsMs = if (url.startsWith("https://")) Random.nextLong(10, 40) else null,
        ttfbMs = Random.nextLong(20, 120),
        downloadMs = Random.nextLong(1, 15),
      ),
    )
    _events.tryEmit(resp)
  }
}

