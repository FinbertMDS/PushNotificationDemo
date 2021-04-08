package com.pushnotification.util;

import android.util.Log;

import com.pushnotification.config.PushyLogging;

public class PushyLogger {
  private static PushyLogListener mListener;
  
  public static void d(String message) {
    Log.d(PushyLogging.TAG, message);
    if (mListener != null)
      mListener.onDebugLog(message); 
  }
  
  public static void d(String message, Exception exc) {
    Log.d(PushyLogging.TAG, message, exc);
    if (mListener != null)
      mListener.onDebugLog(message); 
  }
  
  public static void e(String message) {
    Log.e(PushyLogging.TAG, message);
    if (mListener != null)
      mListener.onErrorLog(message); 
  }
  
  public static void e(String message, Exception exc) {
    Log.e(PushyLogging.TAG, message, exc);
    if (mListener != null)
      mListener.onErrorLog(message); 
  }
  
  public static void setLogListener(PushyLogListener listener) {
    mListener = listener;
  }
  
  public static interface PushyLogListener {
    void onDebugLog(String param1String);
    
    void onErrorLog(String param1String);
  }
}
