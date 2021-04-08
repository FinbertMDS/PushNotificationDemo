package com.pushnotification.util;

import android.content.Context;

public class PushyPreferences {
  public static String getString(String key, String defaultValue, Context context) {
    return PushySingleton.getSettings(context).getString(key, defaultValue);
  }
  
  public static boolean getBoolean(String key, boolean defaultValue, Context context) {
    return PushySingleton.getSettings(context).getBoolean(key, defaultValue);
  }
  
  public static int getInt(String key, int defaultValue, Context context) {
    return PushySingleton.getSettings(context).getInt(key, defaultValue);
  }
  
  public static void saveString(String key, String value, Context context) {
    PushySingleton.getSettings(context).edit().putString(key, value).commit();
  }
  
  public static void remove(String key, Context context) {
    PushySingleton.getSettings(context).edit().remove(key).commit();
  }
  
  public static void saveInt(String key, int value, Context context) {
    PushySingleton.getSettings(context).edit().putInt(key, value).commit();
  }
  
  public static void saveBoolean(String key, boolean value, Context context) {
    PushySingleton.getSettings(context).edit().putBoolean(key, value).commit();
  }
}
