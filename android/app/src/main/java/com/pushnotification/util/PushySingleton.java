package com.pushnotification.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

public class PushySingleton {
  static ObjectMapper mMapper;
  
  static SharedPreferences mSettings;
  
  public static ObjectMapper getJackson() {
    if (mMapper == null) {
      mMapper = new ObjectMapper();
      mMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    } 
    return mMapper;
  }
  
  public static SharedPreferences getSettings(Context context) {
    if (mSettings == null)
      mSettings = PreferenceManager.getDefaultSharedPreferences(context); 
    return mSettings;
  }
}
