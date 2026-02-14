package com.flowtrace.domain.export

import com.flowtrace.domain.redact.RedactionPolicy
import com.flowtrace.domain.session.SessionQuery

interface Exporter {
  suspend fun exportHar(query: SessionQuery, options: ExportOptions): Result<ExportResult>
  suspend fun exportJson(query: SessionQuery, options: ExportOptions): Result<ExportResult>
}

data class ExportOptions(
  val includeBodies: Boolean = true,
  val redactionPolicy: RedactionPolicy = RedactionPolicy(),
)

data class ExportResult(
  val filePath: String,
  val sessionCount: Int,
)


