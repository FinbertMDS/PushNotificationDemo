package com.pushnotification.services;

import android.annotation.TargetApi;
import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import com.pushnotification.config.PushyMQTT;
import org.eclipse.paho.client.mqttv3.MqttSecurityException;

import com.pushnotification.config.PushyPreferenceKeys;
import com.pushnotification.util.PushyAuthentication;
import com.pushnotification.util.PushyLogger;
import com.pushnotification.util.PushyMqttConnection;
import com.pushnotification.util.PushyNetworking;
import com.pushnotification.util.PushyPreferences;
import com.pushnotification.util.exceptions.PushyDateTime;
import com.pushnotification.util.exceptions.PushyFatalException;

@TargetApi(21)
public class PushyJobService extends JobService {
  private static WifiManager mWifiManager;
  
  private static ConnectivityManager mConnectivityManager;
  
  private JobParameters mParams;
  
  private static PushyMqttConnection mSocket;
  
  private static long mLastKeepAlive;
  
  private static long mKeepAliveInterval;
  
  private static long mRetryInterval = PushyMQTT.INITIAL_RETRY_INTERVAL;
  
  public boolean onStartJob(JobParameters params) {
    this.mParams = params;
    if (mWifiManager == null)
      mWifiManager = (WifiManager)getApplicationContext().getSystemService("wifi"); 
    if (mConnectivityManager == null)
      mConnectivityManager = (ConnectivityManager)getApplicationContext().getSystemService("connectivity"); 
    if (mSocket == null)
      mSocket = new PushyMqttConnection((Context)this, mWifiManager, mConnectivityManager, new ConnectionLostRunnable()); 
    String command = params.getExtras().getString("command");
    if (command != null)
      if (command.equals("stop")) {
        PushyLogger.d("Stop requested");
        mSocket.disconnectExistingClientAsync();
        endJob(true);
        return false;
      }  
    if (!mSocket.isConnected()) {
      connect();
    } else if (mSocket.isConnected() && PushyNetworking.getConnectedNetwork(mConnectivityManager) == 1 && mSocket.getNetwork() == 0) {
      (new ReconnectAsync()).execute(new Integer[0]);
    } else if (mLastKeepAlive + mKeepAliveInterval < PushyDateTime.getCurrentTimestamp() + PushyMQTT.MQTT_JOB_TASK_INTERVAL_PADDING) {
      sendKeepAlive();
    } else {
      scheduleJobAgain(true, getJobServiceInterval());
      return false;
    } 
    return true;
  }
  
  void endJob(boolean async) {
    if (async) {
      (new EndJobAsync()).execute(new Integer[0]);
    } else {
      jobFinished(this.mParams, false);
    } 
  }
  
  public class EndJobAsync extends AsyncTask<Integer, String, Integer> {
    protected Integer doInBackground(Integer... parameter) {
      PushyJobService.this.endJob(false);
      return Integer.valueOf(0);
    }
  }
  
  private void connect() {
    if (!PushyNetworking.isNetworkAvailable(mConnectivityManager)) {
      scheduleReconnect();
      return;
    } 
    if (mSocket.isConnecting() || mSocket.isConnected()) {
      scheduleJobAgain(true, getJobServiceInterval());
      return;
    } 
    (new ConnectAsync()).execute(new Integer[0]);
  }
  
  public static boolean isConnected() {
    return (mSocket != null && mSocket.isConnected());
  }
  
  private int getJobServiceInterval() {
    return PushyMQTT.MQTT_DEFAULT_JOB_SERVICE_INTERVAL * 1000;
  }
  
  private void sendKeepAlive() {
    mLastKeepAlive = PushyDateTime.getCurrentTimestamp();
    if (mSocket.isConnected())
      (new SendKeepAliveAsync()).execute(new Integer[0]);
  }
  
  public class SendKeepAliveAsync extends AsyncTask<Integer, String, Integer> {
    protected Integer doInBackground(Integer... parameter) {
      if (!PushyJobService.mSocket.isConnected()) {
        PushyJobService.this.connect();
        return Integer.valueOf(0);
      } 
      PushyLogger.d("PushyJobService: Sending keep alive");
      try {
        PushyJobService.mSocket.sendKeepAlive();
        PushyJobService.this.scheduleJobAgain(false, PushyJobService.this.getJobServiceInterval());
      } catch (Exception e) {
        PushyLogger.d("Keep alive error: " + e.toString(), e);
        PushyJobService.mSocket.disconnectExistingClientSync();
        PushyJobService.this.connect();
      } 
      return Integer.valueOf(0);
    }
  }
  
  public class ConnectAsync extends AsyncTask<Integer, String, Integer> {
    public ConnectAsync() {
      PushyJobService.mSocket.setConnecting(true);
    }
    
    protected Integer doInBackground(Integer... parameter) {
      PushyLogger.d("PushyJobService: Connecting...");
      try {
        if (!PushyPreferences.getBoolean(PushyPreferenceKeys.NOTIFICATIONS_ENABLED, true, (Context)PushyJobService.this))
          throw new PushyFatalException("Notifications have been disabled by the app"); 
        PushyJobService.mSocket.connect();
        PushyJobService.mLastKeepAlive = PushyDateTime.getCurrentTimestamp();
        PushyJobService.mRetryInterval = PushyMQTT.INITIAL_RETRY_INTERVAL;
//        PushyJobService.mSocket;
        PushyJobService.mKeepAliveInterval = PushyMqttConnection.getKeepAliveInterval((Context)PushyJobService.this);
        PushyLogger.d("Connected successfully (sending keep alive every " + PushyMqttConnection.getKeepAliveInterval((Context)PushyJobService.this) + " seconds)");
        PushyJobService.this.scheduleJobAgain(false, PushyJobService.this.getJobServiceInterval());
      } catch (Exception e) {
        PushyLogger.d("Connect exception: " + e.toString(), e);
        if (e.getClass() == PushyFatalException.class) {
          PushyLogger.d("Fatal error encountered, stopping service");
          PushyJobService.this.endJob(false);
          return Integer.valueOf(0);
        } 
        if (e.getClass() == MqttSecurityException.class)
          if (((MqttSecurityException)e).getReasonCode() == 5) {
            PushyLogger.d("MQTT connect returned error code 5, clearing the device credentials");
            PushyAuthentication.clearDeviceCredentials((Context)PushyJobService.this);
            PushyJobService.this.endJob(false);
            return Integer.valueOf(0);
          }  
        PushyJobService.this.scheduleReconnect();
      } finally {
        PushyJobService.mSocket.setConnecting(false);
      } 
      return Integer.valueOf(0);
    }
  }
  
  public class ReconnectAsync extends AsyncTask<Integer, String, Integer> {
    protected Integer doInBackground(Integer... parameter) {
      PushyLogger.d("PushyJobService: Reconnecting due to connectivity change...");
      PushyJobService.mSocket.disconnectExistingClientSync();
      PushyJobService.this.connect();
      return Integer.valueOf(0);
    }
  }
  
  public void scheduleReconnect() {
    if (mRetryInterval < PushyMQTT.MAXIMUM_RETRY_INTERVAL)
      mRetryInterval = Math.min(mRetryInterval * 2L, PushyMQTT.MAXIMUM_RETRY_INTERVAL);
    PushyLogger.d("Reconnecting in " + mRetryInterval + "ms");
    scheduleJobAgain(true, mRetryInterval);
  }
  
  public class ConnectionLostRunnable implements Runnable {
    public void run() {
      PushyJobService.this.connect();
    }
  }
  
  void scheduleJobAgain(boolean async, long interval) {
    if (async) {
      (new ScheduleJobAgainAsync()).execute(new Long[] { Long.valueOf(interval) });
    } else {
      ComponentName serviceName = new ComponentName(getPackageName(), PushyJobService.class.getName());
      JobInfo jobInfo = (new JobInfo.Builder(PushyMQTT.MQTT_JOB_ID, serviceName)).setRequiredNetworkType(1).setMinimumLatency(interval).setOverrideDeadline(interval).build();
      JobScheduler jobScheduler = (JobScheduler)getSystemService("jobscheduler");
      try {
        jobScheduler.schedule(jobInfo);
        endJob(true);
      } catch (Exception exc) {
        PushyLogger.e("JobScheduler error: " + exc.getMessage(), exc);
        (new ScheduleJobAgainAsync()).execute(new Long[] { Long.valueOf(interval) });
      } 
    } 
  }
  
  public class ScheduleJobAgainAsync extends AsyncTask<Long, String, Integer> {
    protected Integer doInBackground(Long... parameter) {
      PushyJobService.this.scheduleJobAgain(false, parameter[0].longValue());
      return Integer.valueOf(0);
    }
  }
  
  public boolean onStopJob(JobParameters params) {
    return false;
  }
}
