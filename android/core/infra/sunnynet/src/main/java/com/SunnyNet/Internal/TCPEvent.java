package com.SunnyNet.Internal;

@SuppressWarnings({"unused", "WeakerAccess"})
public final class TCPEvent {
  public final long sunnyContext;
  public final String localAddress;
  public final String remoteAddress;
  public final long theology;
  public final long messageId;
  public final long type;
  public final long pid;
  public final byte[] data;

  public TCPEvent(
      long sunnyContext,
      String localAddress,
      String remoteAddress,
      long theology,
      long messageId,
      long type,
      long pid,
      byte[] data
  ) {
    this.sunnyContext = sunnyContext;
    this.localAddress = localAddress;
    this.remoteAddress = remoteAddress;
    this.theology = theology;
    this.messageId = messageId;
    this.type = type;
    this.pid = pid;
    this.data = data;
  }
}

