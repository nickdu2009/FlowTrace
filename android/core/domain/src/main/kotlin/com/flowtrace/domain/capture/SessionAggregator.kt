package com.flowtrace.domain.capture

import com.flowtrace.domain.model.SessionId
import com.flowtrace.domain.session.SessionDetail
import com.flowtrace.domain.session.SessionSummary
import kotlinx.coroutines.flow.Flow

/**
 * SessionAggregator — 会话聚合器端口。
 *
 * 负责将乱序、可能缺失/重复的 CaptureEvent 合并为可查询的
 * SessionSummary / SessionDetail。
 *
 * 关键行为要求（见 doc/EVENT_PROTOCOL.md）：
 * - **幂等合并**：同阶段重复事件只更新"更完整/更新鲜"的字段
 * - **乱序容忍**：HttpResponseCompleted 可能先于 HttpRequestStarted 到达
 * - **Body 分离**：聚合器不持有 body 原始数据，只传递 BodyRef
 * - **批处理友好**：支持 batch 输入以减少锁竞争与落盘次数
 */
interface SessionAggregator {

  /**
   * 批量输入事件。实现层负责按 sessionId 分组、合并、更新内存状态。
   *
   * @param events 一批 CaptureEvent（通常来自有界队列的 drain）
   */
  suspend fun ingest(events: List<CaptureEvent>)

  /**
   * 查询指定会话的当前聚合快照。
   */
  suspend fun getSnapshot(sessionId: SessionId): SessionDetail?

  /**
   * 订阅所有会话摘要的变更流（用于驱动 UI 列表）。
   *
   * 实现层应对高频更新做节流/合并，避免 UI 被击穿。
   */
  fun observeSummaries(): Flow<List<SessionSummary>>

  /**
   * 清空所有内存聚合状态（配合"一键清空"功能）。
   */
  suspend fun clear()
}

