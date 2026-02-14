package com.flowtrace.infra.sunnynet

/**
 * SunnyNetBridge is a thin wrapper over JNI bindings.
 *
 * It intentionally does not expose domain models to keep native dependencies isolated.
 */
interface SunnyNetBridge {
  fun setCallback(context: Long, callback: Any): Boolean

  fun createContext(): Long
  fun releaseContext(context: Long): Boolean

  fun setPort(context: Long, port: Int): Boolean
  fun start(context: Long): Boolean
  fun stop(context: Long): Boolean

  fun setGlobalProxy(context: Long, proxy: String, timeoutMs: Int): Boolean

  fun exportCert(context: Long): String
  fun error(context: Long): String

  /**
   * devMode: 0=Proxifier, 1=NFAPI, 2=Tun (Android VPN)
   */
  fun openDrive(context: Long, devMode: Int): Boolean
  fun unDrive(context: Long)

  fun processAll(context: Long, enable: Boolean, stopNetwork: Boolean)
  fun processAddName(context: Long, name: String)
  fun processDelName(context: Long, name: String)
  fun processAddPid(context: Long, pid: Int)
  fun processDelPid(context: Long, pid: Int)

  /**
   * Android Tun(VPN): pass established TUN file descriptor to native.
   *
   * SunnyNet official tun driver reads packets from this fd.
   */
  fun setTunFd(fd: Int): Boolean

  // --- HTTP/WS helpers (best-effort, may be empty) ---
  fun getRequestProto(messageId: Long): String
  fun getResponseProto(messageId: Long): String
  fun getRequestAllHeader(messageId: Long): String
  fun getResponseAllHeader(messageId: Long): String
  fun getResponseStatusCode(messageId: Long): Int
  fun getResponseStatus(messageId: Long): String
  fun getResponseServerAddress(messageId: Long): String
  fun getWebsocketBody(messageId: Long): ByteArray
}


