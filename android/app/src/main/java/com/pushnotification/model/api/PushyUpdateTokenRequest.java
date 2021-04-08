package com.pushnotification.model.api;

import com.pushnotification.model.PushyDeviceCredentials;

public class PushyUpdateTokenRequest {
  public String auth;
  
  public String token;
  
  public String pushToken;
  
  public PushyUpdateTokenRequest(PushyDeviceCredentials credentials, String pushToken) {
    this.auth = credentials.authKey;
    this.token = credentials.token;
    this.pushToken = pushToken;
  }
}
