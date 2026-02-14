package com.flowtrace.domain.capture

/**
 * 网络请求分阶段耗时（单位：毫秒），能拿到多少填多少。
 *
 * 与 doc/EVENT_PROTOCOL.md 对齐：dns / connect / tls / ttfb / download。
 * HAR 导出时可映射为 HAR timings 对象。
 */
data class Timing(
  /** DNS 解析耗时 */
  val dnsMs: Long? = null,
  /** TCP 连接耗时（不含 TLS） */
  val connectMs: Long? = null,
  /** TLS 握手耗时 */
  val tlsMs: Long? = null,
  /** 从请求发出到收到首字节的耗时（Time To First Byte） */
  val ttfbMs: Long? = null,
  /** 响应体下载耗时 */
  val downloadMs: Long? = null,
)

