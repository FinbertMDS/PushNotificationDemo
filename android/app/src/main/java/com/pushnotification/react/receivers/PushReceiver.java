package com.pushnotification.react.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import com.pushnotification.react.services.PushyNotificationService;

public class PushReceiver extends BroadcastReceiver {
  static PushyNotificationService mNotificationService;
  
  public void onReceive(final Context context, final Intent intent) {
    (new Handler(Looper.getMainLooper())).post(new Runnable() {
          public void run() {
            if (PushReceiver.mNotificationService == null)
              PushReceiver.mNotificationService = new PushyNotificationService(context); 
            PushReceiver.mNotificationService.onStartCommand(intent, 0, 0);
          }
        });
  }
}
