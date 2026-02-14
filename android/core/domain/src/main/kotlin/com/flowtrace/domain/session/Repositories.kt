package com.flowtrace.domain.session

import com.flowtrace.domain.model.RuleId
import com.flowtrace.domain.model.SessionId
import kotlinx.coroutines.flow.Flow

interface SessionRepository {
  suspend fun upsertSummary(summary: SessionSummary): Result<Unit>
  suspend fun upsertDetail(sessionId: SessionId, request: HttpMessage?, response: HttpMessage?): Result<Unit>
  suspend fun appendWsMessage(sessionId: SessionId, message: WsMessage): Result<Unit>

  fun observeSummaries(query: SessionQuery): Flow<List<SessionSummary>>
  suspend fun getDetail(sessionId: SessionId): Result<SessionDetail?>

  suspend fun deleteAll(): Result<Unit>
  suspend fun enforceRetention(policy: RetentionPolicy): Result<Unit>

  suspend fun markRuleHit(sessionId: SessionId, ruleId: RuleId, action: String): Result<Unit>
}

data class SessionQuery(
  val keyword: String? = null,
  val host: String? = null,
  val method: String? = null,
  val statusCodeMin: Int? = null,
  val statusCodeMax: Int? = null,
  val appId: String? = null,
  val fromTimeMs: Long? = null,
  val toTimeMs: Long? = null,
  val tlsDecrypted: Boolean? = null,
)

data class RetentionPolicy(
  val maxSessions: Int = 5000,
  val maxTotalBytes: Long = 200L * 1024L * 1024L, // 200MB
)

