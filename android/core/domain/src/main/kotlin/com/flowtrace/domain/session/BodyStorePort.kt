package com.flowtrace.domain.session

/**
 * BodyStorePort — Body 分离存储的端口接口。
 *
 * Domain 层只操作 BodyRef（索引/引用），实际 I/O（文件读写、截断、清理）
 * 由 Infrastructure 层实现并注入。
 *
 * 见 doc/adr/ADR-0003-shared-core-kmp.md 与 doc/ARCHITECTURE.md。
 */
interface BodyStorePort {

  /**
   * 写入 body 数据，返回 BodyRef。
   * 实现层负责：截断（超过 maxBytes 时标记 truncated）、落盘路径管理。
   *
   * @param key 建议使用 sessionId + 方向（req/resp/ws-frame-N）组成唯一 key
   * @param data body 原始字节
   * @param maxBytes 单条 body 最大保存上限（超过则截断）
   */
  suspend fun write(key: String, data: ByteArray, maxBytes: Long): Result<BodyRef>

  /**
   * 按需读取 body 全量（或截断后的保存部分）。
   */
  suspend fun read(ref: BodyRef): Result<ByteArray>

  /**
   * 删除指定 body 文件。
   */
  suspend fun delete(ref: BodyRef): Result<Unit>

  /**
   * 按保留策略清理过期/超限的 body 文件。
   */
  suspend fun enforceRetention(maxTotalBytes: Long): Result<Long>
}
