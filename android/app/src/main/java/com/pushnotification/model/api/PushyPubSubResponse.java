package com.pushnotification.model.api;

import com.fasterxml.jackson.annotation.JsonProperty;

public class PushyPubSubResponse {
  @JsonProperty("success")
  public boolean success;
  
  @JsonProperty("error")
  public String error;
}
