package com.pushnotification.util;

import android.content.Context;
import java.io.IOException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.pushnotification.config.PushyPreferenceKeys;
import com.pushnotification.model.PushyDeviceCredentials;
import com.pushnotification.model.api.PushyAuthRequest;
import com.pushnotification.model.api.PushyAuthResponse;
import com.pushnotification.util.exceptions.PushyException;
import com.pushnotification.util.exceptions.PushyJsonParseException;

public class PushyAuthentication {
  public static PushyDeviceCredentials getDeviceCredentials(Context context) {
    PushyDeviceCredentials credentials = new PushyDeviceCredentials();
    credentials.token = PushyPreferences.getString(PushyPersistence.getEnvironmentPreferenceKey(PushyPreferenceKeys.DEVICE_TOKEN, context), null, context);
    credentials.authKey = PushyPreferences.getString(PushyPersistence.getEnvironmentPreferenceKey(PushyPreferenceKeys.DEVICE_AUTH_KEY, context), null, context);
    String tokenPath = PushyPersistence.getEnvironmentExternalStoragePath("registration.id", context);
    String authKeyPath = PushyPersistence.getEnvironmentExternalStoragePath("registration.key", context);
    if (credentials.token == null || credentials.authKey == null)
      try {
        if (PushyIO.fileExists(tokenPath))
          credentials.token = PushyIO.readFromFile(tokenPath, context); 
        if (PushyIO.fileExists(authKeyPath))
          credentials.authKey = PushyIO.readFromFile(authKeyPath, context); 
        if (!PushyStringUtils.stringIsNullOrEmpty(credentials.token) && !PushyStringUtils.stringIsNullOrEmpty(credentials.authKey)) {
          saveDeviceCredentials(credentials, context);
          if (PushyPreferences.getBoolean(PushyPreferenceKeys.FCM_ENABLED, false, context))
            PushyFirebase.register(context); 
        } 
      } catch (Exception exception) {} 
    if (credentials.token == null || credentials.authKey == null)
      return null; 
    return credentials;
  }
  
  public static void clearDeviceCredentials(Context context) {
    PushyPreferences.remove(PushyPersistence.getEnvironmentPreferenceKey(PushyPreferenceKeys.DEVICE_TOKEN, context), context);
    PushyPreferences.remove(PushyPersistence.getEnvironmentPreferenceKey(PushyPreferenceKeys.DEVICE_AUTH_KEY, context), context);
  }
  
  public static void saveDeviceCredentials(PushyDeviceCredentials credentials, Context context) {
    PushyPreferences.saveString(PushyPersistence.getEnvironmentPreferenceKey(PushyPreferenceKeys.DEVICE_TOKEN, context), credentials.token, context);
    PushyPreferences.saveString(PushyPersistence.getEnvironmentPreferenceKey(PushyPreferenceKeys.DEVICE_AUTH_KEY, context), credentials.authKey, context);
  }
  
  public static boolean validateCredentials(PushyDeviceCredentials credentials, Context context) throws PushyException {
    String json;
    PushyAuthResponse response;
    try {
      json = PushySingleton.getJackson().writeValueAsString(new PushyAuthRequest(credentials, PushyPersistence.getAndroidId(context)));
    } catch (JsonProcessingException exc) {
      throw new PushyJsonParseException(exc.getMessage());
    } 
    String jsonResponse = PushyHTTP.post(PushyEndpoints.getAPIEndpoint(context) + "/devices/auth", json, context);
    try {
      response = (PushyAuthResponse)PushySingleton.getJackson().readValue(jsonResponse, PushyAuthResponse.class);
    } catch (IOException exc) {
      throw new PushyJsonParseException(exc.getMessage());
    } 
    if (!PushyStringUtils.stringIsNullOrEmpty(response.error)) {
      PushyLogger.e("Device auth validation failed: " + response.error);
      return false;
    } 
    if (!response.success)
      return false; 
    return true;
  }
}
