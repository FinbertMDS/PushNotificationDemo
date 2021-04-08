package com.pushnotification.util;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.PersistableBundle;
import com.pushnotification.config.PushyMQTT;
import com.pushnotification.config.PushyPreferenceKeys;
import com.pushnotification.services.PushyJobService;
import com.pushnotification.services.PushySocketService;
import com.pushnotification.util.exceptions.PushyException;

public class PushyServiceManager {
  public static void start(final Context context) {
    if (PushyAuthentication.getDeviceCredentials(context) == null)
      return; 
    if (!PushyPreferences.getBoolean(PushyPreferenceKeys.NOTIFICATIONS_ENABLED, true, context)) {
      PushyLogger.d("Notifications have been disabled by the app");
      return;
    } 
    if (usingJobService() && !PushySocketService.isForegroundServiceEnabled(context)) {
      executeJobCommand("start", context);
    } else {
      startSocketServiceWithAction(context, PushySocketService.ACTION_START);
    } 
    if (PushyPowerSaving.isChineseManufacturer())
      PushyPowerSaving.scheduleRecurringAlarm(context); 
    if (!PushyPreferences.getBoolean(PushyPreferenceKeys.ANDROID_ID_PERSISTED, false, context))
      if (PushyPersistence.getAndroidId(context) != null)
        AsyncTask.execute(new Runnable() {
              public void run() {
                try {
                  if (PushyAuthentication.validateCredentials(PushyAuthentication.getDeviceCredentials(context), context)) {
                    PushyPreferences.saveBoolean(PushyPreferenceKeys.ANDROID_ID_PERSISTED, true, context);
                    PushyLogger.d("Android ID persisted successfully");
                  } 
                } catch (PushyException e) {
                  PushyLogger.e("Failed to persist Android ID in background thread, will retry later");
                } 
              }
            });  
  }
  
  private static boolean usingJobService() {
    return (Build.VERSION.SDK_INT >= 21);
  }
  
  public static void stop(Context context) {
    if (usingJobService()) {
      executeJobCommand("stop", context);
    } else {
      context.stopService(new Intent(context, PushySocketService.class));
    } 
  }
  
  private static void startSocketServiceWithAction(Context context, String action) {
    Intent actionIntent = new Intent(context, PushySocketService.class);
    actionIntent.setAction(action);
    if (PushySocketService.isForegroundServiceEnabled(context) && Build.VERSION.SDK_INT >= 26) {
      context.startForegroundService(actionIntent);
    } else {
      context.startService(actionIntent);
    } 
  }
  
  @TargetApi(21)
  private static void executeJobCommand(String command, Context context) {
    ComponentName serviceName = new ComponentName(context.getPackageName(), PushyJobService.class.getName());
    PersistableBundle extras = new PersistableBundle();
    extras.putString("command", command);
    JobInfo jobInfo = (new JobInfo.Builder(PushyMQTT.MQTT_JOB_ID, serviceName)).setExtras(extras).setRequiredNetworkType(1).setMinimumLatency(1L).setOverrideDeadline(1L).build();
    JobScheduler jobScheduler = (JobScheduler)context.getSystemService("jobscheduler");
    try {
      jobScheduler.schedule(jobInfo);
    } catch (IllegalArgumentException exc) {
      String errorMessage = "Pushy SDK 1.0.35 and up requires 'PushyJobService' to be defined in the AndroidManifest.xml: https://bit.ly/2O3fHEX";
      PushyLogger.e(errorMessage);
      if (context instanceof android.app.Activity)
        try {
          (new AlertDialog.Builder(context)).setTitle("Error").setMessage(errorMessage).create().show();
        } catch (Exception exception) {} 
    } 
  }
}
