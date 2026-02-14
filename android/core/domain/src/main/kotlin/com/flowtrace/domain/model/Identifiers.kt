package com.flowtrace.domain.model

@JvmInline
value class SessionId(val value: String)

@JvmInline
value class AppId(val value: String)

@JvmInline
value class RuleId(val value: String)

/**
 * WebSocket 消息方向，统一定义于 model 包，供 capture 与 session 两个子域共用。
 */
enum class WsDirection { CLIENT_TO_SERVER, SERVER_TO_CLIENT }
