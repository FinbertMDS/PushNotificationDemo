package com.pushnotification.model.api;

import com.fasterxml.jackson.annotation.JsonProperty;

public class PushyAuthResponse {
  @JsonProperty("success")
  public boolean success;
  
  @JsonProperty("error")
  public String error;
}
