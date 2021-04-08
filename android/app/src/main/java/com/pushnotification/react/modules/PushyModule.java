package com.pushnotification.react.modules;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.media.RingtoneManager;
import android.os.AsyncTask;
import com.facebook.react.bridge.ActivityEventListener;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.pushnotification.Pushy;
import com.pushnotification.react.util.PushyMapUtils;
import com.pushnotification.react.util.PushyPersistence;
import com.pushnotification.util.PushyLogger;
import com.pushnotification.util.PushyStringUtils;
import com.pushnotification.util.exceptions.PushyException;
import org.json.JSONObject;

public class PushyModule extends ReactContextBaseJavaModule implements ActivityEventListener {
  public PushyModule(ReactApplicationContext reactContext) {
    super(reactContext);
    reactContext.addActivityEventListener(this);
  }
  
  public String getName() {
    return "PushyModule";
  }
  
  @ReactMethod
  public void notify(String title, String text, ReadableMap payload) {
    ReactApplicationContext reactApplicationContext = getReactApplicationContext();
    Notification.Builder builder = (new Notification.Builder((Context)reactApplicationContext)).setSmallIcon(getNotificationIcon((Context)reactApplicationContext)).setContentTitle(title).setContentText(text).setAutoCancel(true).setVibrate(new long[] { 0L, 400L, 250L, 400L }).setContentIntent(getMainActivityPendingIntent((Context)reactApplicationContext, payload)).setSound(RingtoneManager.getDefaultUri(2));
    NotificationManager notificationManager = (NotificationManager)reactApplicationContext.getSystemService("notification");
    Pushy.setNotificationChannel(builder, (Context)reactApplicationContext);
    notificationManager.notify(text.hashCode(), builder.build());
  }
  
  @ReactMethod
  public void register(final Promise promise) {
    AsyncTask.execute(new Runnable() {
          public void run() {
            try {
              String deviceToken = Pushy.register((Context)PushyModule.this.getReactApplicationContext());
              promise.resolve(deviceToken);
            } catch (PushyException exc) {
              promise.reject((Throwable)exc);
            } 
          }
        });
  }
  
  @ReactMethod
  public void hideNotifications() {
    getReactApplicationContext();
    NotificationManager notificationManager = (NotificationManager)getReactApplicationContext().getSystemService("notification");
    notificationManager.cancelAll();
  }
  
  @ReactMethod
  public void listen() {
    Pushy.listen((Context)getReactApplicationContext());
    if (getReactApplicationContext() == null || getReactApplicationContext().getCurrentActivity() == null || getReactApplicationContext().getCurrentActivity().getIntent() == null)
      return; 
    onNotificationClicked(getReactApplicationContext().getCurrentActivity().getIntent());
  }
  
  @ReactMethod
  public void subscribe(final String topic, final Promise promise) {
    AsyncTask.execute(new Runnable() {
          public void run() {
            try {
              Pushy.subscribe(topic, (Context)PushyModule.this.getReactApplicationContext());
              promise.resolve(Boolean.valueOf(true));
            } catch (PushyException exc) {
              promise.reject((Throwable)exc);
            } 
          }
        });
  }
  
  @ReactMethod
  public void unsubscribe(final String topic, final Promise promise) {
    AsyncTask.execute(new Runnable() {
          public void run() {
            try {
              Pushy.unsubscribe(topic, (Context)PushyModule.this.getReactApplicationContext());
              promise.resolve(Boolean.valueOf(true));
            } catch (PushyException exc) {
              promise.reject((Throwable)exc);
            } 
          }
        });
  }
  
  @ReactMethod
  public void togglePermissionVerification(boolean value) {
    Pushy.togglePermissionVerification(value, (Context)getReactApplicationContext());
  }
  
  @ReactMethod
  public void toggleDirectConnectivity(boolean value) {
    Pushy.toggleDirectConnectivity(value, (Context)getReactApplicationContext());
  }
  
  @ReactMethod
  public void toggleNotifications(boolean value) {
    Pushy.toggleNotifications(value, (Context)getReactApplicationContext());
  }
  
  @ReactMethod
  public void setHeartbeatInterval(int seconds) {
    Pushy.setHeartbeatInterval(seconds, (Context)getReactApplicationContext());
  }
  
  @ReactMethod
  public void setEnterpriseConfig(String apiEndpoint, String mqttEndpoint) {
    Pushy.setEnterpriseConfig(apiEndpoint, mqttEndpoint, (Context)getReactApplicationContext());
  }
  
  @ReactMethod
  public void toggleFCM(boolean value) {
    Pushy.toggleFCM(value, (Context)getReactApplicationContext());
  }
  
  @ReactMethod
  public void setProxyEndpoint(String proxyEndpoint) {
    Pushy.setProxyEndpoint(proxyEndpoint, (Context)getReactApplicationContext());
  }
  
  @ReactMethod
  public void setEnterpriseCertificate(String enterpriseCert) {
    Pushy.setEnterpriseCertificate(enterpriseCert, (Context)getReactApplicationContext());
  }
  
  @ReactMethod
  public void isRegistered(Promise promise) {
    promise.resolve(Boolean.valueOf(Pushy.isRegistered((Context)getReactApplicationContext())));
  }
  
  @ReactMethod
  public void getDeviceCredentials(Promise promise) {
    promise.resolve(Pushy.getDeviceCredentials((Context)getReactApplicationContext()));
  }
  
  private PendingIntent getMainActivityPendingIntent(Context context, ReadableMap payload) {
    Intent launchIntent = context.getPackageManager().getLaunchIntentForPackage(context.getApplicationContext().getPackageName());
    launchIntent.addFlags(603979776);
    String json = "{}";
    try {
      json = PushyMapUtils.convertMapToJson(payload).toString();
    } catch (Exception e) {
      PushyLogger.e("Failed to convert ReadableMap into JSON string", e);
    } 
    launchIntent.putExtra("_pushyNotificationClicked", true);
    launchIntent.putExtra("_pushyNotificationPayload", json);
    return PendingIntent.getActivity(context, json.hashCode(), launchIntent, 67108864);
  }
  
  @ReactMethod
  public void setNotificationIcon(String iconResourceName) {
    PushyPersistence.setNotificationIcon(iconResourceName, (Context)getReactApplicationContext());
  }
  
  private int getNotificationIcon(Context context) {
    String icon = PushyPersistence.getNotificationIcon(context);
    if (icon != null) {
      Resources resources = context.getResources();
      String packageName = context.getPackageName();
      int iconId = resources.getIdentifier(icon, "drawable", packageName);
      if (iconId != 0)
        return iconId; 
      iconId = resources.getIdentifier(icon, "mipmap", packageName);
      if (iconId != 0)
        return iconId; 
    } 
    return 17301659;
  }
  
  void onNotificationClicked(Intent intent) {
    if (!intent.getBooleanExtra("_pushyNotificationClicked", false))
      return; 
    String payload = intent.getStringExtra("_pushyNotificationPayload");
    if (PushyStringUtils.stringIsNullOrEmpty(payload))
      return; 
    try {
      JSONObject jsonObject = new JSONObject(payload);
      WritableMap map = PushyMapUtils.convertJsonToMap(jsonObject);
      ((DeviceEventManagerModule.RCTDeviceEventEmitter)getReactApplicationContext()
        .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class))
        .emit("NotificationClick", map);
    } catch (Exception e) {
      PushyLogger.e("Failed to parse JSON into WritableMap", e);
    } 
  }
  
  public void onActivityResult(Activity activity, int requestCode, int resultCode, Intent data) {}
  
  public void onNewIntent(Intent intent) {
    onNotificationClicked(intent);
  }
}
