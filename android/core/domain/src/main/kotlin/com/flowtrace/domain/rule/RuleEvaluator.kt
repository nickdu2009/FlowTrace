package com.flowtrace.domain.rule

import com.flowtrace.domain.capture.CaptureEvent

interface RuleEvaluator {
  fun evaluate(rules: List<Rule>, event: CaptureEvent): RuleDecision?
}

data class RuleDecision(
  val rule: Rule,
  val action: RuleAction,
)

