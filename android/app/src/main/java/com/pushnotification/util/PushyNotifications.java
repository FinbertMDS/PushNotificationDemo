package com.pushnotification.util;

import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import android.util.Log;

import com.pushnotification.config.PushyLogging;
import com.pushnotification.config.PushyNotificationChannel;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

public class PushyNotifications {
  public static void setNotificationChannel(Object builder, Context context) {
    if (Build.VERSION.SDK_INT < 26)
      return; 
    NotificationManager notificationManager = (NotificationManager)context.getSystemService("notification");
    String channelId = PushyNotificationChannel.CHANNEL_ID;
    String channelName = PushyNotificationChannel.CHANNEL_NAME;
    int channelImportance = PushyNotificationChannel.CHANNEL_IMPORTANCE;
    try {
      Class<?> notificationChannelClass = Class.forName("android.app.NotificationChannel");
      Constructor<?> notificationChannelConstructor = notificationChannelClass.getDeclaredConstructor(new Class[] { String.class, CharSequence.class, int.class });
      Object notificationChannel = notificationChannelConstructor.newInstance(new Object[] { channelId, channelName, Integer.valueOf(channelImportance) });
      Method createNotificationChannelMethod = notificationManager.getClass().getDeclaredMethod("createNotificationChannel", new Class[] { notificationChannelClass });
      createNotificationChannelMethod.invoke(notificationManager, new Object[] { notificationChannel });
      Method setChannelIdMethod = builder.getClass().getDeclaredMethod("setChannelId", new Class[] { String.class });
      setChannelIdMethod.invoke(builder, new Object[] { channelId });
      Log.d(PushyLogging.TAG, "Notification channel set successfully");
    } catch (Exception exc) {
      Log.e(PushyLogging.TAG, "Creating/setting notification channel failed", exc);
    } 
  }
}
