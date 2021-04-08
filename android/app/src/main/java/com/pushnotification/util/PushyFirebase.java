package com.pushnotification.util;

import android.content.Context;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;

public class PushyFirebase {
  public static void register(final Context context) {
    if (GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context) != 0) {
      PushyLogger.e("Google Play Services is not available for this device");
      return;
    } 
    FirebaseInstanceId.getInstance().getInstanceId()
      .addOnCompleteListener(new OnCompleteListener<InstanceIdResult>() {
          public void onComplete(Task<InstanceIdResult> task) {
            if (!task.isSuccessful()) {
              PushyLogger.e("Firebase registration failed", task.getException());
              return;
            } 
            final String token = ((InstanceIdResult)task.getResult()).getToken();
            PushyLogger.d("FCM device token: " + token);
            (new Thread(new Runnable() {
                  public void run() {
                    try {
                      PushyAPI.setFCMToken(token, context);
                    } catch (Exception e) {
                      PushyLogger.e(e.getMessage(), e);
                    } 
                  }
                })).start();
          }
        });
  }
}
