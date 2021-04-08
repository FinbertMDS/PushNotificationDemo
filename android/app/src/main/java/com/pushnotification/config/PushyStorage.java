package com.pushnotification.config;

import android.os.Environment;

public class PushyStorage {
  public static final String EXTERNAL_STORAGE_APP_ID_FILE = "app.id";
  
  public static final String EXTERNAL_STORAGE_TOKEN_FILE = "registration.id";
  
  public static final String EXTERNAL_STORAGE_AUTH_KEY_FILE = "registration.key";
  
  public static final String EXTERNAL_STORAGE_FILE_ENTERPRISE_PREFIX = "enterprise.";
  
  public static final String EXTERNAL_STORAGE_DIRECTORY = Environment.getExternalStorageDirectory() + "/Android/data/com.pushnotification/";
}
