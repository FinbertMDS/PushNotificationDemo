package com.pushnotification.model.api;

import com.pushnotification.model.PushyDeviceCredentials;

public class PushyPubSubRequest {
  public String auth;
  
  public String token;
  
  public String[] topics;
  
  public PushyPubSubRequest(String[] topics, PushyDeviceCredentials credentials) {
    this.topics = topics;
    this.auth = credentials.authKey;
    this.token = credentials.token;
  }
}
