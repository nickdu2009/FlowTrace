package com.flowtrace.domain.rule

import com.flowtrace.domain.model.RuleId
import kotlinx.coroutines.flow.Flow

interface RuleRepository {
  fun observeRules(): Flow<List<Rule>>
  suspend fun upsert(rule: Rule): Result<Unit>
  suspend fun delete(id: RuleId): Result<Unit>
}


