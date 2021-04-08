package com.pushnotification.util.exceptions;

public class PushyDateTime {
  public static long getCurrentTimestamp() {
    return System.currentTimeMillis() / 1000L;
  }
}
