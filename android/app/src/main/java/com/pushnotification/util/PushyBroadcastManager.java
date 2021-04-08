package com.pushnotification.util;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.util.Log;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.pushnotification.config.PushyBroadcast;
import com.pushnotification.config.PushyLogging;
import com.pushnotification.config.PushyPayloadKeys;
import com.pushnotification.config.PushyPreferenceKeys;
import com.pushnotification.model.PushyBroadcastReceiver;

public class PushyBroadcastManager {
  static List<PushyBroadcastReceiver> mCachedPushReceivers;
  
  public static void publishNotification(Context context, Map<String, Object> payload, String json) throws Exception {
    Object pushyId = payload.get(PushyPayloadKeys.PUSHY_ID);
    Object pushyPayload = payload.get(PushyPayloadKeys.PUSHY_PAYLOAD);
    if (pushyId != null) {
      String key = PushyPreferenceKeys.PUSH_ID_RECEIVED_PREFIX + pushyId.toString();
      if (PushyPreferences.getBoolean(key, false, context)) {
        PushyLogger.d("Ignoring duplicate notification");
        return;
      } 
      PushyPreferences.saveBoolean(key, true, context);
    } 
    if (pushyPayload != null) {
      json = pushyPayload.toString();
      payload = (Map<String, Object>)PushySingleton.getJackson().readValue(json, Map.class);
    } 
    Intent intent = new Intent();
    for (Map.Entry<String, Object> entry : payload.entrySet()) {
      if (entry.getValue() == null)
        continue; 
      if (entry.getValue().getClass() == String.class)
        intent.putExtra(entry.getKey(), (String)entry.getValue()); 
      if (entry.getValue().getClass() == Boolean.class)
        intent.putExtra(entry.getKey(), (Boolean)entry.getValue()); 
      if (entry.getValue().getClass() == Integer.class)
        intent.putExtra(entry.getKey(), (Integer)entry.getValue()); 
      if (entry.getValue().getClass() == Long.class)
        intent.putExtra(entry.getKey(), (Long)entry.getValue()); 
      if (entry.getValue().getClass() == Double.class || entry.getValue().getClass() == Float.class)
        intent.putExtra(entry.getKey(), (Double)entry.getValue()); 
      if (entry.getValue().getClass() == ArrayList.class)
        intent.putExtra(entry.getKey(), (Serializable)((ArrayList)entry.getValue()).toArray()); 
    } 
    if (json != null)
      intent.putExtra("__json", json); 
    intent.setPackage(context.getPackageName());
    intent.setAction(PushyBroadcast.ACTION);
    sendBroadcast(context, intent);
  }
  
  static void sendBroadcast(Context context, Intent intent) {
    if (mCachedPushReceivers != null && mCachedPushReceivers.size() > 0 && mCachedPushReceivers.get(0) != null) {
      for (PushyBroadcastReceiver receiver : mCachedPushReceivers) {
        if (receiver != null) {
          Log.d(PushyLogging.TAG, "Invoking cached push receiver via reflection: " + receiver.getReceiver().getClass().getName());
          receiver.execute(context, intent);
        } 
      } 
      return;
    } 
    mCachedPushReceivers = new ArrayList<>();
    PackageManager pm = context.getPackageManager();
    List<ResolveInfo> matches = pm.queryBroadcastReceivers(intent, 0);
    if (matches.size() == 0) {
      PushyLogger.e("No suitable push BroadcastReceiver declared in AndroidManifest.xml matching the following intent filter: pushy.me");
      return;
    } 
    for (ResolveInfo resolveInfo : matches) {
      try {
        Class<?> receiverClass = Class.forName(resolveInfo.activityInfo.name);
        Constructor<?> receiverConstructor = receiverClass.getDeclaredConstructor(new Class[0]);
        BroadcastReceiver broadcastReceiver = (BroadcastReceiver)receiverConstructor.newInstance(new Object[0]);
        Method onReceiveMethod = broadcastReceiver.getClass().getDeclaredMethod("onReceive", new Class[] { Context.class, Intent.class });
        PushyBroadcastReceiver receiver = new PushyBroadcastReceiver(broadcastReceiver, onReceiveMethod);
        mCachedPushReceivers.add(receiver);
        Log.d(PushyLogging.TAG, "Invoking push receiver via reflection: " + resolveInfo.activityInfo.name);
        receiver.execute(context, intent);
      } catch (Exception exc) {
        Log.e(PushyLogging.TAG, "Invoking push receiver via reflection failed", exc);
      } 
    } 
  }
}
