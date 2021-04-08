package com.pushnotification.util;

import android.net.ConnectivityManager;
import android.net.NetworkInfo;

public class PushyNetworking {
  public static int getConnectedNetwork(ConnectivityManager manager) {
    NetworkInfo wifi = manager.getNetworkInfo(1);
    NetworkInfo mobile = manager.getNetworkInfo(0);
    if (wifi != null && wifi.isConnected())
      return 1; 
    if (mobile != null && mobile.isConnected())
      return 0; 
    return -1;
  }
  
  public static boolean isNetworkAvailable(ConnectivityManager connectivityManager) {
    NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
    return (activeNetworkInfo != null);
  }
}
