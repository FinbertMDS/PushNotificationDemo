package com.pushnotification.model.api;

import com.pushnotification.model.PushyDeviceCredentials;

public class PushyAuthRequest {
  public int sdk;
  
  public String auth;
  
  public String token;
  
  public String androidId;
  
  public PushyAuthRequest(PushyDeviceCredentials credentials, String androidId) {
    this.auth = credentials.authKey;
    this.token = credentials.token;
    this.sdk = 1068;
    if (androidId != null)
      this.androidId = androidId; 
  }
}
