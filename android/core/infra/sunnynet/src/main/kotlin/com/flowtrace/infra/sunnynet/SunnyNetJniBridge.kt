package com.flowtrace.infra.sunnynet

import com.SunnyNet.api
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

  override fun setCallback(context: Long, callback: Any): Boolean =
    if (!available) false else runCatching { api.SunnyNetSetCallback(context, callback) }.getOrDefault(false)

  override fun createContext(): Long = if (!available) 0L else runCatching { api.CreateSunnyNet() }.getOrDefault(0L)
  override fun releaseContext(context: Long): Boolean =
    if (!available) false else runCatching { api.ReleaseSunnyNet(context) }.getOrDefault(false)

  override fun setPort(context: Long, port: Int): Boolean =
    if (!available) false else runCatching { api.SunnyNetSetPort(context, port.toLong()) }.getOrDefault(false)

  override fun start(context: Long): Boolean = if (!available) false else runCatching { api.SunnyNetStart(context) }.getOrDefault(false)
  override fun stop(context: Long): Boolean = if (!available) false else runCatching { api.SunnyNetClose(context) }.getOrDefault(false)

  override fun setGlobalProxy(context: Long, proxy: String, timeoutMs: Int): Boolean =
    if (!available) false else runCatching { api.SetGlobalProxy(context, proxy, timeoutMs.toLong()) }.getOrDefault(false)

  override fun exportCert(context: Long): String = if (!available) "" else runCatching { api.ExportCert(context) }.getOrDefault("")
  override fun error(context: Long): String = if (!available) "native-not-loaded" else runCatching { api.SunnyNetError(context) }.getOrDefault("")

  override fun openDrive(context: Long, devMode: Int): Boolean =
    if (!available) false else runCatching { api.OpenDrive(context, devMode.toLong()) }.getOrDefault(false)
  override fun unDrive(context: Long) {
    if (!available) return
    runCatching { api.UnDrive(context) }
  }

  override fun processAll(context: Long, enable: Boolean, stopNetwork: Boolean) {
    if (!available) return
    runCatching { api.ProcessALLName(context, enable, stopNetwork) }
  }

  override fun processAddName(context: Long, name: String) {
    if (!available) return
    runCatching { api.ProcessAddName(context, name) }
  }

  override fun processDelName(context: Long, name: String) {
    if (!available) return
    runCatching { api.ProcessDelName(context, name) }
  }

  override fun processAddPid(context: Long, pid: Int) {
    if (!available) return
    runCatching { api.ProcessAddPid(context, pid.toLong()) }
  }

  override fun processDelPid(context: Long, pid: Int) {
    if (!available) return
    runCatching { api.ProcessDelPid(context, pid.toLong()) }
  }

  override fun setTunFd(fd: Int): Boolean =
    if (!available) false else runCatching { api.SetFd(fd.toLong()) }.getOrDefault(false)

  override fun getRequestProto(messageId: Long): String =
    if (!available) "" else runCatching { api.GetRequestProto(messageId) }.getOrDefault("")

  override fun getResponseProto(messageId: Long): String =
    if (!available) "" else runCatching { api.GetResponseProto(messageId) }.getOrDefault("")

  override fun getRequestAllHeader(messageId: Long): String =
    if (!available) "" else runCatching { api.GetRequestAllHeader(messageId) }.getOrDefault("")

  override fun getResponseAllHeader(messageId: Long): String =
    if (!available) "" else runCatching { api.GetResponseAllHeader(messageId) }.getOrDefault("")

  override fun getResponseStatusCode(messageId: Long): Int =
    if (!available) -1 else runCatching { api.GetResponseStatusCode(messageId) }
      .getOrDefault(-1L)
      .toInt()

  override fun getResponseStatus(messageId: Long): String =
    if (!available) "" else runCatching { api.GetResponseStatus(messageId) }.getOrDefault("")

  override fun getResponseServerAddress(messageId: Long): String =
    if (!available) "" else runCatching { api.GetResponseServerAddress(messageId) }.getOrDefault("")

  override fun getWebsocketBody(messageId: Long): ByteArray =
    if (!available) ByteArray(0) else runCatching { api.GetWebsocketBody(messageId) }.getOrDefault(ByteArray(0))

  companion object {
    // TODO(M4): confirm actual library name once SunnyNet .so is provided
    private const val LIB_NAME = "sunnynet"
  }
}

