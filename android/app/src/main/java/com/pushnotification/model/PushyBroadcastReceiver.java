package com.pushnotification.model;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.pushnotification.config.PushyLogging;

import java.lang.reflect.Method;

public class PushyBroadcastReceiver {
  Method mOnReceiveMethod;
  
  BroadcastReceiver mBroadcastReceiver;
  
  public PushyBroadcastReceiver(BroadcastReceiver receiver, Method method) {
    this.mOnReceiveMethod = method;
    this.mBroadcastReceiver = receiver;
  }
  
  public void execute(Context context, Intent intent) {
    try {
      this.mOnReceiveMethod.invoke(this.mBroadcastReceiver, new Object[] { context, intent });
    } catch (Exception exc) {
      Log.e(PushyLogging.TAG, "Invoking push receiver via reflection failed", exc);
    } 
  }
  
  public BroadcastReceiver getReceiver() {
    return this.mBroadcastReceiver;
  }
}
