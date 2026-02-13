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

interface Redactor {
  fun redactHeaders(headers: Map<String, String>, policy: RedactionPolicy): Map<String, String>
  fun redactText(text: String, policy: RedactionPolicy): String
}

