package com.pushnotification.react.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class PushyPersistence {
  public static final String NOTIFICATION_ICON = "pushyNotificationIcon";
  
  private static SharedPreferences getSettings(Context context) {
    return PreferenceManager.getDefaultSharedPreferences(context);
  }
  
  public static void setNotificationIcon(String icon, Context context) {
    getSettings(context).edit().putString("pushyNotificationIcon", icon).commit();
  }
  
  public static String getNotificationIcon(Context context) {
    return getSettings(context).getString("pushyNotificationIcon", null);
  }
}
