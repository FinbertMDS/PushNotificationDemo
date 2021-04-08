package com.pushnotification.util;

import android.content.Context;
import java.util.Map;

import com.pushnotification.config.PushyPayloadKeys;
import com.pushnotification.config.PushyPreferenceKeys;
import com.pushnotification.model.PushyDeviceCredentials;
import com.pushnotification.model.api.PushyPushDeliveryRequest;
import com.pushnotification.model.api.PushyPushDeliveryResponse;
import com.pushnotification.model.api.PushyUpdateTokenRequest;
import com.pushnotification.model.api.PushyUpdateTokenResponse;
import com.pushnotification.util.exceptions.PushyException;

public class PushyAPI {
  public static void setPushDelivered(Map<String, String> payload, Context context) throws Exception {
    String json;
    PushyPushDeliveryResponse response;
    String pushyId = payload.get(PushyPayloadKeys.PUSHY_ID);
    if (pushyId == null) {
      PushyLogger.e("Pushy ID missing from FCM payload");
      return;
    } 
    PushyDeviceCredentials credentials = PushyAuthentication.getDeviceCredentials(context);
    if (credentials == null)
      throw new PushyException("Updating push delivery failed: The device is not registered for push notifications."); 
    try {
      PushyPushDeliveryRequest update = new PushyPushDeliveryRequest(credentials);
      json = PushySingleton.getJackson().writeValueAsString(update);
    } catch (Exception exc) {
      throw new PushyException("Updating push delivery failed due to invalid JSON:" + exc.getMessage());
    } 
    String result = PushyHTTP.post(PushyEndpoints.getAPIEndpoint(context) + "/pushes/" + pushyId + "/delivery", json, context);
    try {
      response = (PushyPushDeliveryResponse)PushySingleton.getJackson().readValue(result, PushyPushDeliveryResponse.class);
    } catch (Exception exc) {
      throw new PushyException("Updating push delivery failed due to invalid response:" + exc.getMessage());
    } 
    if (!PushyStringUtils.stringIsNullOrEmpty(response.error))
      throw new PushyException("Updating push delivery failed: " + response.error); 
    if (!response.success)
      throw new PushyException("Updating push delivery failed: An unexpected response was encountered."); 
    PushyLogger.d("Notification marked as delivered");
  }
  
  public static void setFCMToken(String token, Context context) throws Exception {
    String json;
    PushyUpdateTokenResponse response;
    PushyDeviceCredentials credentials = PushyAuthentication.getDeviceCredentials(context);
    if (credentials == null)
      throw new PushyException("Updating FCM token failed: The device is not registered for push notifications."); 
    try {
      PushyUpdateTokenRequest update = new PushyUpdateTokenRequest(credentials, token);
      json = PushySingleton.getJackson().writeValueAsString(update);
    } catch (Exception exc) {
      throw new PushyException("Update token failed due to invalid JSON:" + exc.getMessage());
    } 
    String result = PushyHTTP.post(PushyEndpoints.getAPIEndpoint(context) + "/devices/token", json, context);
    try {
      response = (PushyUpdateTokenResponse)PushySingleton.getJackson().readValue(result, PushyUpdateTokenResponse.class);
    } catch (Exception exc) {
      throw new PushyException("Updating FCM token failed due to invalid response:" + exc.getMessage());
    } 
    if (!PushyStringUtils.stringIsNullOrEmpty(response.error))
      throw new PushyException("Updating FCM token failed: " + response.error); 
    if (!response.success)
      throw new PushyException("Updating FCM token failed: An unexpected response was encountered."); 
    PushyPreferences.saveString(PushyPreferenceKeys.FCM_TOKEN, token, context);
    PushyLogger.d("FCM device token updated successfully");
  }
}
