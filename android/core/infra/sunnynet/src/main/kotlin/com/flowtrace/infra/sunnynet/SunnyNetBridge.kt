package com.flowtrace.infra.sunnynet

/**
 * SunnyNetBridge is a thin wrapper over JNI bindings.
 *
 * It intentionally does not expose domain models to keep native dependencies isolated.
 */
interface SunnyNetBridge {
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
}

