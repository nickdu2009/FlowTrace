package com.flowtrace.domain.model

/**
 * ClockPort — 时间源的端口接口。
 *
 * 抽象当前时间，便于：
 * - 单元测试中注入固定/可控时间
 * - 双端一致性（Shared Core 不直接调用 System.currentTimeMillis）
 *
 * 见 doc/adr/ADR-0003-shared-core-kmp.md。
 */
interface ClockPort {
  /** 返回当前时间戳（毫秒，UTC epoch） */
  fun nowMillis(): Long
}

