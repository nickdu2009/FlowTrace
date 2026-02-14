package com.flowtrace.domain.redact

data class RedactionPolicy(
  val enabled: Boolean = true,
  val headerKeysToRedact: Set<String> = setOf(
    "authorization",
    "cookie",
    "set-cookie",
  ),
  val headerKeyContainsToRedact: Set<String> = setOf(
    "token",
    "secret",
    "apikey",
    "session",
  ),
)

/**
 * Redactor — 脱敏处理端口。
 *
 * 在入库（Storage）与导出（Exporter）两个环节统一拦截。
 * 见 doc/SECURITY_GUIDE.md 与 doc/adr/ADR-0001-mvp-decisions.md 决策 8。
 */
interface Redactor {
  /**
   * 对 HTTP headers 做脱敏（key 匹配则替换 value 为占位符）。
   * 使用 List<Pair> 以保留同名 header（如 Set-Cookie）。
   */
  fun redactHeaders(
    headers: List<Pair<String, String>>,
    policy: RedactionPolicy,
  ): List<Pair<String, String>>

  /** 对文本内容做脱敏（URL query 中可能含 token 等） */
  fun redactText(text: String, policy: RedactionPolicy): String
}
