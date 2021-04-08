package com.pushnotification.config;

public class PushyMQTT {
  public static final String ENDPOINT = "ssl://mqtt-{ts}.pushy.io:443";
  
  public static final String DIRECT_ENDPOINT = "ssl://mqtt.pushy.io:443";
  
  public static final long INITIAL_RETRY_INTERVAL = 500L;
  
  public static final long MAXIMUM_RETRY_INTERVAL = 60000L;
  
  public static int MQTT_QUALITY_OF_SERVICE = 1;
  
  public static short MQTT_DEFAULT_KEEP_ALIVE = 300;
  
  public static boolean MQTT_RETAINED_PUBLISH = false;
  
  public static short MQTT_ACK_TIMEOUT = 15000;
  
  public static short MQTT_CONNECT_TIMEOUT = 15;
  
  public static int MQTT_JOB_ID = 10000;
  
  public static int MQTT_DEFAULT_JOB_SERVICE_INTERVAL = 15;
  
  public static int MQTT_CHINESE_DEVICE_MAINTENANCE_INTERVAL = 30;
  
  public static int MQTT_JOB_TASK_INTERVAL_PADDING = 2;
  
  public static final int FOREGROUND_NOTIFICATION_ID = 100031;
}
