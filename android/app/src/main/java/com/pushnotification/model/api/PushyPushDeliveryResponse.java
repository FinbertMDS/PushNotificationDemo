package com.pushnotification.model.api;

import com.fasterxml.jackson.annotation.JsonProperty;

public class PushyPushDeliveryResponse {
  @JsonProperty("success")
  public boolean success;
  
  @JsonProperty("error")
  public String error;
}
