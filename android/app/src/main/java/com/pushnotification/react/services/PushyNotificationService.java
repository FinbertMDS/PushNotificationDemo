package com.pushnotification.react.services;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import com.facebook.react.HeadlessJsTaskService;
import com.facebook.react.ReactApplication;
import com.facebook.react.ReactNativeHost;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.jstasks.HeadlessJsTaskConfig;

public class PushyNotificationService extends HeadlessJsTaskService {
  public PushyNotificationService(Context context) {
    attachBaseContext(context);
  }
  
  protected ReactNativeHost getReactNativeHost() {
    return ((ReactApplication)getApplicationContext()).getReactNativeHost();
  }
  
  protected HeadlessJsTaskConfig getTaskConfig(Intent intent) {
    Bundle extras = intent.getExtras();
    if (extras == null)
      return null; 
    return new HeadlessJsTaskConfig("PushyPushReceiver", Arguments.fromBundle(extras), 5000L, true);
  }
}
