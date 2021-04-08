package com.pushnotification;

import android.content.Context;

import com.pushnotification.config.PushyPreferenceKeys;
import com.pushnotification.model.PushyDeviceCredentials;
import com.pushnotification.model.api.PushyRegistrationRequest;
import com.pushnotification.model.api.PushyRegistrationResponse;
import com.pushnotification.services.PushyJobService;
import com.pushnotification.services.PushySocketService;
import com.pushnotification.util.PushyAuthentication;
import com.pushnotification.util.PushyEndpoints;
import com.pushnotification.util.PushyFirebase;
import com.pushnotification.util.PushyHTTP;
import com.pushnotification.util.PushyIO;
import com.pushnotification.util.PushyLogger;
import com.pushnotification.util.PushyNotifications;
import com.pushnotification.util.PushyPermissionVerification;
import com.pushnotification.util.PushyPersistence;
import com.pushnotification.util.PushyPreferences;
import com.pushnotification.util.PushyPubSub;
import com.pushnotification.util.PushyServiceManager;
import com.pushnotification.util.PushySingleton;
import com.pushnotification.util.PushyStringUtils;
import com.pushnotification.util.exceptions.PushyException;
import com.pushnotification.util.exceptions.PushyJsonParseException;
import com.pushnotification.util.exceptions.PushyRegistrationException;

public class Pushy {
  public static void listen(Context context) {
    PushyServiceManager.start(context);
  }
  
  public static void subscribe(String[] topics, Context context) throws PushyException {
    PushyPubSub.subscribe(topics, context);
  }
  
  public static void subscribe(String topic, Context context) throws PushyException {
    subscribe(new String[] { topic }, context);
  }
  
  public static void unsubscribe(String[] topics, Context context) throws PushyException {
    PushyPubSub.unsubscribe(topics, context);
  }
  
  public static void unsubscribe(String topic, Context context) throws PushyException {
    unsubscribe(new String[] { topic }, context);
  }
  
  public static void togglePermissionVerification(boolean value, Context context) {
    PushyPreferences.saveBoolean(PushyPreferenceKeys.PERMISSION_ENFORCEMENT, value, context);
  }
  
  public static void toggleFCM(boolean value, Context context) {
    PushyPreferences.saveBoolean(PushyPreferenceKeys.FCM_ENABLED, value, context);
    if (value && isRegistered(context) && PushyPreferences.getString(PushyPreferenceKeys.FCM_TOKEN, null, context) == null)
      PushyFirebase.register(context); 
  }
  
  public static void setEnterpriseCertificate(String value, Context context) {
    PushyPreferences.saveString(PushyPreferenceKeys.ENTERPRISE_CERTIFICATE, value, context);
  }
  
  public static void toggleDirectConnectivity(boolean value, Context context) {
    PushyPreferences.saveBoolean(PushyPreferenceKeys.DIRECT_CONNECTIVITY, value, context);
  }
  
  public static void setProxyEndpoint(String value, Context context) {
    PushyPreferences.saveString(PushyPreferenceKeys.PROXY_ENDPOINT, value, context);
  }
  
  public static void toggleWifiPolicyCompliance(boolean value, Context context) {
    PushyPreferences.saveBoolean(PushyPreferenceKeys.WIFI_POLICY_COMPLIANCE, value, context);
  }
  
  public static void toggleNotifications(boolean value, Context context) {
    PushyPreferences.saveBoolean(PushyPreferenceKeys.NOTIFICATIONS_ENABLED, value, context);
    if (value) {
      PushyServiceManager.start(context);
    } else {
      PushyServiceManager.stop(context);
    } 
  }
  
  public static void setHeartbeatInterval(int seconds, Context context) {
    if (seconds < 60) {
      seconds = 60;
      PushyLogger.e("The minimum heartbeat interval is 60 seconds.");
    } 
    PushyPreferences.saveInt(PushyPreferenceKeys.KEEPALIVE_INTERVAL, seconds, context);
  }
  
  public static void setEnterpriseConfig(String apiEndpoint, String mqttEndpoint, Context context) {
    if (apiEndpoint != null && apiEndpoint.endsWith("/"))
      apiEndpoint = apiEndpoint.substring(0, apiEndpoint.length() - 1); 
    if (mqttEndpoint != null && mqttEndpoint.endsWith("/"))
      mqttEndpoint = mqttEndpoint.substring(0, mqttEndpoint.length() - 1); 
    if (!PushyStringUtils.equals(PushyPreferences.getString(PushyPreferenceKeys.ENTERPRISE_MQTT_ENDPOINT, null, context), mqttEndpoint)) {
      PushyLogger.d("New enterprise MQTT endpoint, stopping socket service");
      PushyServiceManager.stop(context);
    } 
    PushyPreferences.saveString(PushyPreferenceKeys.ENTERPRISE_API_ENDPOINT, apiEndpoint, context);
    PushyPreferences.saveString(PushyPreferenceKeys.ENTERPRISE_MQTT_ENDPOINT, mqttEndpoint, context);
  }
  
  public static boolean isRegistered(Context context) {
    return (PushyAuthentication.getDeviceCredentials(context) != null);
  }
  
  public static boolean isConnected() {
    return (PushySocketService.isConnected() || PushyJobService.isConnected());
  }
  
  public static void unregister(Context context) {
    PushyAuthentication.clearDeviceCredentials(context);
    PushyServiceManager.stop(context);
  }
  
  public static void setAppId(String appId, Context context) {
    String previousId = PushyPreferences.getString(PushyPreferenceKeys.APP_ID, "", context);
    if (PushyStringUtils.stringIsNullOrEmpty(previousId)) {
      String appIdPath = PushyPersistence.getEnvironmentExternalStoragePath("app.id", context);
      if (PushyIO.fileExists(appIdPath))
        try {
          previousId = PushyIO.readFromFile(appIdPath, context);
        } catch (Exception e) {
          PushyLogger.d("Fetching app ID from external storage failed");
        }  
    } 
    if (previousId != null && !previousId.equals(appId))
      if (isRegistered(context))
        unregister(context);  
    PushyPreferences.saveString(PushyPreferenceKeys.APP_ID, appId, context);
  }
  
  public static String register(Context context) throws PushyException {
    String json;
    PushyRegistrationResponse response;
    PushyPermissionVerification.verifyManifestPermissions(context);
    PushyDeviceCredentials persistedCredentials = PushyAuthentication.getDeviceCredentials(context);
    if (persistedCredentials != null)
      if (PushyAuthentication.validateCredentials(persistedCredentials, context)) {
        if (PushyPreferences.getBoolean(PushyPreferenceKeys.FCM_ENABLED, false, context))
          PushyFirebase.register(context); 
        listen(context);
        return persistedCredentials.token;
      }  
    String androidId = PushyPersistence.getAndroidId(context);
    try {
      PushyRegistrationRequest pushyRegistrationRequest = new PushyRegistrationRequest();
      String appId = PushyPreferences.getString(PushyPreferenceKeys.APP_ID, null, context);
      if (androidId != null)
        pushyRegistrationRequest.androidId = androidId; 
      if (appId != null) {
        pushyRegistrationRequest.appId = appId;
      } else {
        pushyRegistrationRequest.app = context.getPackageName();
      } 
      json = PushySingleton.getJackson().writeValueAsString(pushyRegistrationRequest);
    } catch (Exception exc) {
      throw new PushyJsonParseException(exc.getMessage());
    } 
    String register = PushyHTTP.post(PushyEndpoints.getAPIEndpoint(context) + "/register", json, context);
    try {
      response = (PushyRegistrationResponse)PushySingleton.getJackson().readValue(register, PushyRegistrationResponse.class);
    } catch (Exception exc) {
      throw new PushyJsonParseException(exc.getMessage());
    } 
    if (!PushyStringUtils.stringIsNullOrEmpty(response.error))
      throw new PushyRegistrationException("Registration failed: " + response.error); 
    if (response.token == null || response.auth == null)
      throw new PushyRegistrationException("Registration failed, please try again later."); 
    if (androidId != null)
      PushyPreferences.saveBoolean(PushyPreferenceKeys.ANDROID_ID_PERSISTED, true, context);
    PushyAuthentication.saveDeviceCredentials(new PushyDeviceCredentials(response.token, response.auth), context);
    PushyLogger.d("Pushy registration success: " + response.token);
    listen(context);
    if (PushyPreferences.getBoolean(PushyPreferenceKeys.FCM_ENABLED, false, context))
      PushyFirebase.register(context); 
    return response.token;
  }
  
  public static PushyDeviceCredentials getDeviceCredentials(Context context) {
    return PushyAuthentication.getDeviceCredentials(context);
  }
  
  public static void setDeviceCredentials(PushyDeviceCredentials credentials, Context context) throws PushyException {
    if (PushyStringUtils.stringIsNullOrEmpty(credentials.token) || PushyStringUtils.stringIsNullOrEmpty(credentials.authKey))
      throw new PushyException("Please provide both the device token and auth key."); 
    if (!PushyAuthentication.validateCredentials(credentials, context))
      throw new PushyException("Authentication failed, please double-check the provided credentials."); 
    PushyAuthentication.saveDeviceCredentials(credentials, context);
    listen(context);
  }
  
  public static void setNotificationChannel(Object builder, Context context) {
    PushyNotifications.setNotificationChannel(builder, context);
  }
}
