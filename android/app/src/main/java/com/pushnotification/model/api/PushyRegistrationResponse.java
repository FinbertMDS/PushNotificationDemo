package com.pushnotification.model.api;

import com.fasterxml.jackson.annotation.JsonProperty;

public class PushyRegistrationResponse {
  @JsonProperty("token")
  public String token;
  
  @JsonProperty("auth")
  public String auth;
  
  @JsonProperty("error")
  public String error;
}
