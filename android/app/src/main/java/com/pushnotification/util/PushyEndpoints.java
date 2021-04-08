package com.pushnotification.util;

import android.content.Context;

import com.pushnotification.config.PushyAPIConfig;
import com.pushnotification.config.PushyMQTT;
import com.pushnotification.config.PushyPreferenceKeys;

public class PushyEndpoints {
  public static String getAPIEndpoint(Context context) {
    String proxyEndpoint = PushyPreferences.getString(PushyPreferenceKeys.PROXY_ENDPOINT, null, context);
    if (proxyEndpoint != null)
      return "https://" + proxyEndpoint; 
    return PushyPreferences.getString(PushyPreferenceKeys.ENTERPRISE_API_ENDPOINT, PushyAPIConfig.ENDPOINT, context);
  }
  
  public static String getMQTTEndpoint(Context context) throws Exception {
    String enterpriseEndpoint = PushyPreferences.getString(PushyPreferenceKeys.ENTERPRISE_MQTT_ENDPOINT, null, context);
    if (enterpriseEndpoint != null)
      return enterpriseEndpoint; 
    String proxyEndpoint = PushyPreferences.getString(PushyPreferenceKeys.PROXY_ENDPOINT, null, context);
    if (proxyEndpoint != null)
      return "ssl://" + proxyEndpoint; 
    boolean directConnectivity = PushyPreferences.getBoolean(PushyPreferenceKeys.DIRECT_CONNECTIVITY, false, context);
    if (directConnectivity)
      return PushyMQTT.DIRECT_ENDPOINT;
    String brokerEndpoint = PushyMQTT.ENDPOINT;
    if (brokerEndpoint.contains("{ts}"))
      brokerEndpoint = brokerEndpoint.replace("{ts}", String.valueOf(System.currentTimeMillis() / 1000L)); 
    return brokerEndpoint;
  }
}
