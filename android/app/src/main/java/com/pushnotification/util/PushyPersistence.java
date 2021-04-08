package com.pushnotification.util;

import android.content.Context;
import android.os.Build;
import android.provider.Settings;

import com.pushnotification.config.PushyPreferenceKeys;
import com.pushnotification.config.PushyStorage;

public class PushyPersistence {
  public static String getAndroidId(Context context) {
    if (Build.VERSION.SDK_INT < 21)
      return null; 
    String androidId = Settings.Secure.getString(context.getContentResolver(), "android_id");
    if (androidId == null)
      return androidId; 
    androidId = androidId + "-" + Build.MANUFACTURER + "-" + Build.MODEL;
    androidId = androidId.replaceAll(" ", "-");
    return androidId;
  }
  
  public static String getEnvironmentPreferenceKey(String originalKey, Context context) {
    if (PushyEnterprise.isConfigured(context))
      return originalKey + PushyPreferenceKeys.ENTERPRISE_KEY_SUFFIX;
    return originalKey;
  }
  
  public static String getEnvironmentExternalStoragePath(String fileName, Context context) {
    if (PushyEnterprise.isConfigured(context))
      fileName = "enterprise." + fileName; 
    return PushyStorage.EXTERNAL_STORAGE_DIRECTORY + context.getPackageName() + "/" + fileName;
  }
}
