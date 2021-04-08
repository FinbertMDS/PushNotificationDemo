package com.pushnotification.services;

import android.content.Context;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import java.util.Collections;
import java.util.Map;
import com.pushnotification.Pushy;
import com.pushnotification.config.PushyPreferenceKeys;
import com.pushnotification.util.PushyAPI;
import com.pushnotification.util.PushyBroadcastManager;
import com.pushnotification.util.PushyLogger;
import com.pushnotification.util.PushyPreferences;

public class PushyFirebaseService extends FirebaseMessagingService {
  public void onMessageReceived(RemoteMessage remoteMessage) {
    Map<String, String> payload = remoteMessage.getData();
    if (payload == null || payload.size() == 0) {
      PushyLogger.e("Received empty push from FCM");
      return;
    } 
    if (!Pushy.isRegistered((Context)this)) {
      PushyLogger.d("FCM onMessageReceived() called when device is not registered");
      return;
    } 
    if (!PushyPreferences.getBoolean(PushyPreferenceKeys.NOTIFICATIONS_ENABLED, true, (Context)this)) {
      PushyLogger.d("FCM onMessageReceived() called when notifications have been toggled off");
      return;
    } 
    PushyLogger.d("Received push via FCM for package " + getPackageName() + "\n" + payload);
    try {
      PushyBroadcastManager.publishNotification((Context)this, Collections.unmodifiableMap(payload), null);
      PushyAPI.setPushDelivered(payload, (Context)this);
    } catch (Exception exc) {
      PushyLogger.e("Publishing notification failed: " + exc.getMessage(), exc);
    } 
  }
  
  public void onNewToken(String token) {
    if (!Pushy.isRegistered((Context)this)) {
      PushyLogger.d("FCM onNewToken() called when device not yet registered");
      return;
    } 
    if (!PushyPreferences.getBoolean(PushyPreferenceKeys.FCM_ENABLED, false, (Context)this)) {
      PushyLogger.d("FCM onNewToken() called when FCM fallback is not enabled");
      return;
    } 
    if (token.equals(PushyPreferences.getString(PushyPreferenceKeys.FCM_TOKEN, "", (Context)this))) {
      PushyLogger.d("FCM onNewToken() called with old token");
      return;
    } 
    PushyLogger.d("FCM device token refreshed: " + token);
    try {
      PushyAPI.setFCMToken(token, (Context)this);
    } catch (Exception e) {
      PushyLogger.e(e.getMessage(), e);
    } 
  }
}
