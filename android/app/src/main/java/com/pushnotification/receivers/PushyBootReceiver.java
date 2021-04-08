package com.pushnotification.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import com.pushnotification.util.PushyLogger;
import com.pushnotification.util.PushyServiceManager;

public class PushyBootReceiver extends BroadcastReceiver {
  public void onReceive(Context context, Intent intent) {
    PushyLogger.d("Device boot complete");
    PushyServiceManager.start(context);
  }
}
