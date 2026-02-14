package com.flowtrace.domain.rule

import com.flowtrace.domain.model.AppId
import com.flowtrace.domain.model.RuleId

data class Rule(
  val id: RuleId,
  val name: String,
  val enabled: Boolean,
  val priority: Int,
  val match: RuleMatch,
  val action: RuleAction,
)

data class RuleMatch(
  val appId: AppId? = null,
  val host: String? = null,
  val urlPrefix: String? = null,
  val method: String? = null,
)

sealed interface RuleAction {
  data object Allow : RuleAction
  data object Block : RuleAction

  data class RewriteHeaders(
    val set: Map<String, String> = emptyMap(),
    val remove: Set<String> = emptySet(),
  ) : RuleAction

  data class HostMap(
    val ip: String,
    val port: Int,
  ) : RuleAction

  /**
   * MVP: Only apply to text/ * (any subtype) or application/json.
   */
  data class ReplaceBody(
    val contentTypePrefix: String,
    val bodyUtf8: String,
  ) : RuleAction
}

