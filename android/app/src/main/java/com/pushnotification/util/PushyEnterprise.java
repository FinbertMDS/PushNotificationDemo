package com.pushnotification.util;

import android.content.Context;

import com.pushnotification.config.PushyPreferenceKeys;

public class PushyEnterprise {
  public static boolean isConfigured(Context context) {
    return (PushyPreferences.getString(PushyPreferenceKeys.ENTERPRISE_API_ENDPOINT, null, context) != null);
  }
}
