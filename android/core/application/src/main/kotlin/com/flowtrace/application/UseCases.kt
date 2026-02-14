package com.flowtrace.application

import com.flowtrace.domain.capture.CaptureConfig
import com.flowtrace.domain.capture.CaptureEngine
import com.flowtrace.domain.export.ExportOptions
import com.flowtrace.domain.export.Exporter
import com.flowtrace.domain.rule.Rule
import com.flowtrace.domain.rule.RuleRepository
import com.flowtrace.domain.session.SessionQuery
import com.flowtrace.domain.session.SessionRepository

class StartCaptureUseCase(
  private val engine: CaptureEngine,
) {
  suspend operator fun invoke(config: CaptureConfig): Result<Unit> = engine.start(config)
}

class StopCaptureUseCase(
  private val engine: CaptureEngine,
) {
  suspend operator fun invoke(): Result<Unit> = engine.stop()
}

class ObserveSessionsUseCase(
  private val sessions: SessionRepository,
) {
  operator fun invoke(query: SessionQuery) = sessions.observeSummaries(query)
}

class GetSessionDetailUseCase(
  private val sessions: SessionRepository,
) {
  suspend operator fun invoke(sessionId: com.flowtrace.domain.model.SessionId) = sessions.getDetail(sessionId)
}

class UpsertRuleUseCase(
  private val rules: RuleRepository,
) {
  suspend operator fun invoke(rule: Rule) = rules.upsert(rule)
}

class ExportHarUseCase(
  private val exporter: Exporter,
) {
  suspend operator fun invoke(query: SessionQuery, options: ExportOptions) = exporter.exportHar(query, options)
}

class ExportJsonUseCase(
  private val exporter: Exporter,
) {
  suspend operator fun invoke(query: SessionQuery, options: ExportOptions) = exporter.exportJson(query, options)
}


