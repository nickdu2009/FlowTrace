package com.SunnyNet;

/**
 * SunnyNet official JNI entrypoint.
 *
 * IMPORTANT:
 * - Class name and package MUST match SunnyNet's exported JNI symbols:
 *   Java_com_SunnyNet_api_*
 * - Do NOT rename this class.
 *
 * This class intentionally does NOT load the native library. Loading and graceful
 * degradation are handled by FlowTrace's bridge layer.
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public final class api {
  private api() {}

  // --- lifecycle ---
  public static native long CreateSunnyNet();
  public static native boolean ReleaseSunnyNet(long sunnyContext);

  public static native boolean SunnyNetSetPort(long sunnyContext, long port);
  public static native boolean SunnyNetStart(long sunnyContext);
  public static native boolean SunnyNetClose(long sunnyContext);

  // --- callbacks ---
  public static native boolean SunnyNetSetCallback(long sunnyContext, Object callback);

  // --- driver / process selection ---
  public static native boolean OpenDrive(long sunnyContext, long devMode);
  public static native void UnDrive(long sunnyContext);

  public static native void ProcessALLName(long sunnyContext, boolean open, boolean stopNetwork);
  public static native void ProcessAddName(long sunnyContext, String name);
  public static native void ProcessDelName(long sunnyContext, String name);
  public static native void ProcessAddPid(long sunnyContext, long pid);
  public static native void ProcessDelPid(long sunnyContext, long pid);

  // --- global proxy / errors / cert ---
  public static native boolean SetGlobalProxy(long sunnyContext, String proxyAddress, long timeoutMs);
  public static native String SunnyNetError(long sunnyContext);
  public static native String ExportCert(long sunnyContext);

  // --- Android Tun(VPN) fd (best-effort; symbol name must exist in .so) ---
  public static native boolean SetFd(long fd);

  // --- HTTP / WS helpers ---
  public static native String GetRequestProto(long messageId);
  public static native String GetResponseProto(long messageId);
  public static native String GetRequestAllHeader(long messageId);
  public static native String GetResponseAllHeader(long messageId);
  public static native long GetResponseStatusCode(long messageId);
  public static native String GetResponseStatus(long messageId);
  public static native String GetResponseServerAddress(long messageId);

  public static native byte[] GetWebsocketBody(long messageId);
}

