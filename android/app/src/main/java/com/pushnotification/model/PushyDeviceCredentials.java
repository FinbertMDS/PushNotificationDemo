package com.pushnotification.model;

public class PushyDeviceCredentials {
  public String token;
  
  public String authKey;
  
  public PushyDeviceCredentials() {}
  
  public PushyDeviceCredentials(String token, String authKey) {
    this.token = token;
    this.authKey = authKey;
  }
}
