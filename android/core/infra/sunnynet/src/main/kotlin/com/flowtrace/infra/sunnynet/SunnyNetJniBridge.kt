package com.flowtrace.infra.sunnynet

import javax.inject.Inject
import javax.inject.Singleton
import timber.log.Timber

/**
 * JNI bridge implementation for SunnyNet.
 *
 * MVP/M4 goal:
 * - Provide a compile-safe API surface
 * - Gracefully degrade when native library is missing
 *
 * Real native integration (method bodies / callbacks) will be completed once
 * SunnyNet .so is available and the JNI signatures are confirmed.
 */
@Singleton
class SunnyNetJniBridge @Inject constructor() : SunnyNetBridge {

  private val available: Boolean = runCatching {
    System.loadLibrary(LIB_NAME)
    true
  }.onFailure {
    Timber.w(it, "SunnyNet native library not loaded; falling back to Fake engine")
  }.getOrDefault(false)

  override fun createContext(): Long = if (!available) 0L else nCreateContext()
  override fun releaseContext(context: Long): Boolean = if (!available) false else nReleaseContext(context)

  override fun setPort(context: Long, port: Int): Boolean = if (!available) false else nSetPort(context, port)
  override fun start(context: Long): Boolean = if (!available) false else nStart(context)
  override fun stop(context: Long): Boolean = if (!available) false else nStop(context)

  override fun setGlobalProxy(context: Long, proxy: String, timeoutMs: Int): Boolean =
    if (!available) false else nSetGlobalProxy(context, proxy, timeoutMs)

  override fun exportCert(context: Long): String = if (!available) "" else nExportCert(context)
  override fun error(context: Long): String = if (!available) "native-not-loaded" else nError(context)

  override fun openDrive(context: Long, devMode: Int): Boolean = if (!available) false else nOpenDrive(context, devMode)
  override fun unDrive(context: Long) {
    if (!available) return
    nUnDrive(context)
  }

  override fun processAll(context: Long, enable: Boolean, stopNetwork: Boolean) {
    if (!available) return
    nProcessAll(context, enable, stopNetwork)
  }

  override fun processAddName(context: Long, name: String) {
    if (!available) return
    nProcessAddName(context, name)
  }

  override fun processDelName(context: Long, name: String) {
    if (!available) return
    nProcessDelName(context, name)
  }

  override fun processAddPid(context: Long, pid: Int) {
    if (!available) return
    nProcessAddPid(context, pid)
  }

  override fun processDelPid(context: Long, pid: Int) {
    if (!available) return
    nProcessDelPid(context, pid)
  }

  // --- Native method declarations ---
  private external fun nCreateContext(): Long
  private external fun nReleaseContext(context: Long): Boolean
  private external fun nSetPort(context: Long, port: Int): Boolean
  private external fun nStart(context: Long): Boolean
  private external fun nStop(context: Long): Boolean
  private external fun nSetGlobalProxy(context: Long, proxy: String, timeoutMs: Int): Boolean
  private external fun nExportCert(context: Long): String
  private external fun nError(context: Long): String
  private external fun nOpenDrive(context: Long, devMode: Int): Boolean
  private external fun nUnDrive(context: Long)
  private external fun nProcessAll(context: Long, enable: Boolean, stopNetwork: Boolean)
  private external fun nProcessAddName(context: Long, name: String)
  private external fun nProcessDelName(context: Long, name: String)
  private external fun nProcessAddPid(context: Long, pid: Int)
  private external fun nProcessDelPid(context: Long, pid: Int)

  companion object {
    // TODO(M4): confirm actual library name once SunnyNet .so is provided
    private const val LIB_NAME = "sunnynet"
  }
}

