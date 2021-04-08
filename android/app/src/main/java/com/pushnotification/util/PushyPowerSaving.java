package com.pushnotification.util;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import com.pushnotification.config.PushyMQTT;
import com.pushnotification.receivers.PushyUpdateReceiver;

public class PushyPowerSaving {
  public static boolean isChineseManufacturer() {
    String[] brands = { "xiaomi", "huawei", "oneplus", "oppo", "vivo", "asus", "sony", "honor" };
    for (String brand : brands) {
      if (Build.BRAND.toLowerCase().contains(brand))
        return true; 
    } 
    return false;
  }
  
  public static void scheduleRecurringAlarm(Context context) {
    if (Build.VERSION.SDK_INT >= 23) {
      AlarmManager alarmManager = (AlarmManager)context.getSystemService("alarm");
      long when = System.currentTimeMillis() + (1000 * PushyMQTT.MQTT_CHINESE_DEVICE_MAINTENANCE_INTERVAL);
      Intent alarmIntent = new Intent(context, PushyUpdateReceiver.class);
      PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, alarmIntent, 268435456);
      alarmManager.setExactAndAllowWhileIdle(0, when, pendingIntent);
    } 
  }
}
