package com.pushnotification.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import com.pushnotification.util.PushyServiceManager;

public class PushyUpdateReceiver extends BroadcastReceiver {
  public void onReceive(Context context, Intent intent) {
    PushyServiceManager.start(context);
  }
}
