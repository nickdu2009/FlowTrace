package com.SunnyNet.Internal;

@SuppressWarnings({"unused", "WeakerAccess"})
public final class WebSocketEvent {
  public final long sunnyContext;
  public final long theology;
  public final long messageId;
  public final long type;
  public final String method;
  public final String url;
  public final long pid;
  public final long messageType;

  public WebSocketEvent(
      long sunnyContext,
      long theology,
      long messageId,
      long type,
      String method,
      String url,
      long pid,
      long messageType
  ) {
    this.sunnyContext = sunnyContext;
    this.theology = theology;
    this.messageId = messageId;
    this.type = type;
    this.method = method;
    this.url = url;
    this.pid = pid;
    this.messageType = messageType;
  }
}

