package com.SunnyNet.Internal;

@SuppressWarnings({"unused", "WeakerAccess"})
public final class UDPEvent {
  public final long sunnyContext;
  public final String localAddress;
  public final String remoteAddress;
  public final long theology;
  public final long messageId;
  public final long type;
  public final long pid;

  public UDPEvent(
      long sunnyContext,
      String localAddress,
      String remoteAddress,
      long theology,
      long messageId,
      long type,
      long pid
  ) {
    this.sunnyContext = sunnyContext;
    this.localAddress = localAddress;
    this.remoteAddress = remoteAddress;
    this.theology = theology;
    this.messageId = messageId;
    this.type = type;
    this.pid = pid;
  }
}

