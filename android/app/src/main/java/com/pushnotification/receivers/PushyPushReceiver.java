package com.pushnotification.receivers;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;

public class PushyPushReceiver extends BroadcastReceiver {
  @SuppressLint({"NewApi"})
  public void onReceive(Context context, Intent intent) {
    String notificationTitle = getAppName(context);
    String notificationText = "";
    if (intent.getStringExtra("message") != null)
      notificationText = intent.getStringExtra("message"); 
    Notification.Builder builder = (new Notification.Builder(context)).setAutoCancel(true).setContentTitle(notificationTitle).setContentText(notificationText).setVibrate(new long[] { 0L, 400L, 250L, 400L }).setSmallIcon((context.getApplicationInfo()).icon).setSound(RingtoneManager.getDefaultUri(2)).setContentIntent(getMainActivityPendingIntent(context));
    NotificationManager notificationManager = (NotificationManager)context.getSystemService("notification");
    notificationManager.notify(1, builder.build());
  }
  
  private static String getAppName(Context context) {
    return context.getPackageManager().getApplicationLabel(context.getApplicationInfo()).toString();
  }
  
  public static PendingIntent getMainActivityPendingIntent(Context context) {
    Intent launchIntent = context.getPackageManager().getLaunchIntentForPackage(context.getApplicationContext().getPackageName());
    launchIntent.addFlags(603979776);
    return PendingIntent.getActivity(context, 0, launchIntent, 134217728);
  }
}
