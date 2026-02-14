package com.SunnyNet.Internal;

@SuppressWarnings({"unused", "WeakerAccess"})
public final class HTTPEvent {
  public final long sunnyContext;
  public final long theology;
  public final long messageId;
  public final long type;
  public final String method;
  public final String url;
  public final String error;
  public final long pid;

  public HTTPEvent(
      long sunnyContext,
      long theology,
      long messageId,
      long type,
      String method,
      String url,
      String error,
      long pid
  ) {
    this.sunnyContext = sunnyContext;
    this.theology = theology;
    this.messageId = messageId;
    this.type = type;
    this.method = method;
    this.url = url;
    this.error = error;
    this.pid = pid;
  }
}

