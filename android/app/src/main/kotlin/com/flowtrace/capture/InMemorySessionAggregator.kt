package com.flowtrace.capture

import com.flowtrace.domain.capture.CaptureEvent
import com.flowtrace.domain.capture.HttpRequestFailed
import com.flowtrace.domain.capture.HttpRequestStarted
import com.flowtrace.domain.capture.HttpResponseCompleted
import com.flowtrace.domain.capture.SessionAggregator
import com.flowtrace.domain.capture.WsConnected
import com.flowtrace.domain.capture.WsDisconnected
import com.flowtrace.domain.capture.WsMessageFrame
import com.flowtrace.domain.model.SessionId
import com.flowtrace.domain.session.HttpMessage
import com.flowtrace.domain.session.SessionDetail
import com.flowtrace.domain.session.SessionSummary
import com.flowtrace.domain.session.WsMessage
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * MVP-0 in-memory aggregator:
 * - idempotent-ish merge by overwriting missing fields
 * - out-of-order tolerant (response may arrive before request)
 *
 * Persistence will be added later (Room + Body files).
 */
@Singleton
class InMemorySessionAggregator @Inject constructor() : SessionAggregator {

  private data class MutableSession(
    var createdAtMs: Long = 0L,
    var appId: com.flowtrace.domain.model.AppId? = null,
    var processName: String? = null,
    var host: String? = null,
    var scheme: String? = null,
    var method: String? = null,
    var path: String? = null,
    var statusCode: Int? = null,
    var tlsDecrypted: Boolean = false,
    var pinningSuspected: Boolean = false,
    var request: HttpMessage? = null,
    var response: HttpMessage? = null,
    var websocketMessages: MutableList<WsMessage> = mutableListOf(),
    var timing: com.flowtrace.domain.capture.Timing? = null,
  )

  private val sessions = LinkedHashMap<String, MutableSession>(1024, 0.75f, true)
  private val _summaries = MutableStateFlow<List<SessionSummary>>(emptyList())
  override fun observeSummaries(): Flow<List<SessionSummary>> = _summaries.asStateFlow()

  override suspend fun ingest(events: List<CaptureEvent>) {
    for (e in events) {
      val key = e.sessionId.value
      val s = sessions.getOrPut(key) {
        MutableSession(createdAtMs = e.timestampMs)
      }

      // common fields
      if (s.createdAtMs == 0L) s.createdAtMs = e.timestampMs
      if (s.appId == null) s.appId = e.appId
      if (s.processName == null) s.processName = e.processName

      when (e) {
        is HttpRequestStarted -> {
          s.host = s.host ?: e.host
          s.method = s.method ?: e.method
          s.scheme = s.scheme ?: e.url?.substringBefore("://")
          s.path = s.path ?: e.url?.substringAfter("://")?.substringAfter("/", missingDelimiterValue = "/")
          s.request = s.request ?: HttpMessage(
            protocol = e.protocol,
            url = e.url,
            headers = e.headers,
            body = e.body,
          )
        }

        is HttpResponseCompleted -> {
          s.host = s.host ?: e.host
          s.method = s.method ?: e.method
          s.statusCode = s.statusCode ?: e.statusCode
          s.tlsDecrypted = s.tlsDecrypted || e.tlsDecrypted
          s.timing = s.timing ?: e.timing
          s.response = s.response ?: HttpMessage(
            protocol = e.protocol,
            url = e.url,
            headers = e.headers,
            body = e.body,
          )
        }

        is HttpRequestFailed -> {
          s.host = s.host ?: e.host
          s.method = s.method ?: e.method
          s.statusCode = s.statusCode ?: -1
          // best-effort heuristic: https + fail => maybe pinning/untrusted
          val isHttps = (e.url ?: "").startsWith("https://")
          if (isHttps) s.pinningSuspected = true
        }

        is WsConnected -> {
          s.host = s.host ?: e.url?.substringAfter("://")?.substringBefore("/")
          s.scheme = s.scheme ?: e.url?.substringBefore("://")
        }

        is WsMessageFrame -> {
          s.websocketMessages.add(
            WsMessage(
              timestampMs = e.timestampMs,
              direction = e.direction,
              messageType = e.messageType,
              body = e.body,
            )
          )
        }

        is WsDisconnected -> {
          // no-op for MVP-0
        }

        else -> Unit
      }
    }

    // Emit summaries (bounded to latest N)
    val list = sessions.entries
      .toList()
      .takeLast(200) // MVP-0 keep UI light
      .map { entry ->
        val id = entry.key
        val s = entry.value
        SessionSummary(
          id = SessionId(id),
          appId = s.appId,
          processName = s.processName,
          host = s.host,
          scheme = s.scheme,
          method = s.method,
          path = s.path,
          statusCode = s.statusCode,
          durationMs = null,
          sentBytes = null,
          receivedBytes = null,
          tlsDecrypted = s.tlsDecrypted,
          pinningSuspected = s.pinningSuspected,
          ruleHit = null,
          createdAtMs = s.createdAtMs,
        )
      }
      .sortedByDescending { summary -> summary.createdAtMs }

    _summaries.value = list
  }

  override suspend fun getSnapshot(sessionId: SessionId): SessionDetail? {
    val s = sessions[sessionId.value] ?: return null
    val summary = SessionSummary(
      id = sessionId,
      appId = s.appId,
      processName = s.processName,
      host = s.host,
      scheme = s.scheme,
      method = s.method,
      path = s.path,
      statusCode = s.statusCode,
      durationMs = null,
      sentBytes = null,
      receivedBytes = null,
      tlsDecrypted = s.tlsDecrypted,
      pinningSuspected = s.pinningSuspected,
      ruleHit = null,
      createdAtMs = s.createdAtMs,
    )

    return SessionDetail(
      summary = summary,
      request = s.request,
      response = s.response,
      websocketMessages = s.websocketMessages.toList(),
      timing = s.timing,
      notes = emptyMap(),
    )
  }

  override suspend fun clear() {
    sessions.clear()
    _summaries.value = emptyList()
  }
}

