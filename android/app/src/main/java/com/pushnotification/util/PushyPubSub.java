package com.pushnotification.util;

import android.content.Context;
import com.pushnotification.model.PushyDeviceCredentials;
import com.pushnotification.model.api.PushyPubSubRequest;
import com.pushnotification.model.api.PushyPubSubResponse;
import com.pushnotification.util.exceptions.PushyException;
import com.pushnotification.util.exceptions.PushyPubSubException;

public class PushyPubSub {
  public static void subscribe(String[] topics, Context context) throws PushyException {
    String json;
    PushyPubSubResponse response;
    PushyDeviceCredentials credentials = PushyAuthentication.getDeviceCredentials(context);
    if (credentials == null)
      throw new PushyPubSubException("Subscribe failed: The device is not registered for push notifications."); 
    try {
      PushyPubSubRequest subscribe = new PushyPubSubRequest(topics, credentials);
      json = PushySingleton.getJackson().writeValueAsString(subscribe);
    } catch (Exception exc) {
      throw new PushyPubSubException("Subscribe failed due to invalid JSON:" + exc.getMessage());
    } 
    String result = PushyHTTP.post(PushyEndpoints.getAPIEndpoint(context) + "/devices/subscribe", json, context);
    try {
      response = (PushyPubSubResponse)PushySingleton.getJackson().readValue(result, PushyPubSubResponse.class);
    } catch (Exception exc) {
      throw new PushyPubSubException("Subscribe failed due to invalid response:" + exc.getMessage());
    } 
    if (!PushyStringUtils.stringIsNullOrEmpty(response.error))
      throw new PushyPubSubException("Subscribe failed: " + response.error); 
    if (!response.success)
      throw new PushyPubSubException("Subscribe failed: An unexpected response was encountered."); 
  }
  
  public static void unsubscribe(String[] topics, Context context) throws PushyException {
    String json;
    PushyPubSubResponse response;
    PushyDeviceCredentials credentials = PushyAuthentication.getDeviceCredentials(context);
    if (credentials == null)
      throw new PushyPubSubException("Unsubscribe failed: The device is not registered for push notifications."); 
    try {
      PushyPubSubRequest unsubscribe = new PushyPubSubRequest(topics, credentials);
      json = PushySingleton.getJackson().writeValueAsString(unsubscribe);
    } catch (Exception exc) {
      throw new PushyPubSubException("Unsubscribe failed due to invalid JSON:" + exc.getMessage());
    } 
    String result = PushyHTTP.post(PushyEndpoints.getAPIEndpoint(context) + "/devices/unsubscribe", json, context);
    try {
      response = (PushyPubSubResponse)PushySingleton.getJackson().readValue(result, PushyPubSubResponse.class);
    } catch (Exception exc) {
      throw new PushyPubSubException("Unsubscribe failed due to invalid response:" + exc.getMessage());
    } 
    if (!PushyStringUtils.stringIsNullOrEmpty(response.error))
      throw new PushyPubSubException("Unsubscribe failed: " + response.error); 
    if (!response.success)
      throw new PushyPubSubException("Unsubscribe failed: An unexpected response was encountered."); 
  }
}
