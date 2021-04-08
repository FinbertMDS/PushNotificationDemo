package com.pushnotification.model.api;

import com.pushnotification.model.PushyDeviceCredentials;

public class PushyPushDeliveryRequest {
  public String auth;
  
  public String token;
  
  public PushyPushDeliveryRequest(PushyDeviceCredentials credentials) {
    this.auth = credentials.authKey;
    this.token = credentials.token;
  }
}
