package com.pushnotification.services;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.IBinder;
import android.os.Parcelable;
import android.os.SystemClock;
import org.eclipse.paho.client.mqttv3.MqttSecurityException;

import com.pushnotification.config.PushyMQTT;
import com.pushnotification.config.PushyPreferenceKeys;
import com.pushnotification.receivers.PushyPushReceiver;
import com.pushnotification.util.PushyAuthentication;
import com.pushnotification.util.PushyLogger;
import com.pushnotification.util.PushyMqttConnection;
import com.pushnotification.util.PushyNetworking;
import com.pushnotification.util.PushyParcelableUtil;
import com.pushnotification.util.PushyPreferences;
import com.pushnotification.util.PushySingleton;
import com.pushnotification.util.exceptions.PushyFatalException;

public class PushySocketService extends Service {
  private WifiManager mWifiManager;
  
  private AlarmManager mAlarmManager;
  
  private ConnectivityManager mConnectivityManager;
  
  private boolean mIsDestroyed;
  
  private static PushyMqttConnection mSocket;
  
  private long mRetryInterval = PushyMQTT.INITIAL_RETRY_INTERVAL;
  
  public static final String ACTION_START = "Pushy.START";
  
  public static final String ACTION_RECONNECT = "Pushy.RECONNECT";
  
  public static final String ACTION_KEEP_ALIVE = "Pushy.KEEP_ALIVE";
  
  public void onCreate() {
    super.onCreate();
    PushyLogger.d("Creating service");
    this.mWifiManager = (WifiManager)getApplicationContext().getSystemService("wifi");
    this.mAlarmManager = (AlarmManager)getApplicationContext().getSystemService("alarm");
    this.mConnectivityManager = (ConnectivityManager)getApplicationContext().getSystemService("connectivity");
    mSocket = new PushyMqttConnection((Context)this, this.mWifiManager, this.mConnectivityManager, new ConnectionLostRunnable());
    handleCrashedService();
    start();
    registerReceiver(this.mConnectivityListener, new IntentFilter("android.net.conn.CONNECTIVITY_CHANGE"));
    (new StartForegroundAsync()).execute(new Integer[0]);
  }
  
  public class ConnectionLostRunnable implements Runnable {
    public void run() {
      PushySocketService.this.reconnectAsync();
    }
  }
  
  public static boolean isConnected() {
    return (mSocket != null && mSocket.isConnected());
  }
  
  public static void setForegroundNotification(Notification notification, Context context) {
    String result = null;
    if (notification != null) {
      notification.contentIntent = null;
      result = PushyParcelableUtil.byteArrayToString(PushyParcelableUtil.marshall((Parcelable)notification));
    } 
    PushySingleton.getSettings(context).edit().putString(PushyPreferenceKeys.FOREGROUND_NOTIFICATION, result).commit();
  }
  
  public static boolean isForegroundServiceEnabled(Context context) {
    return (PushySingleton.getSettings(context).getString(PushyPreferenceKeys.FOREGROUND_NOTIFICATION, null) != null);
  }
  
  private static Notification getForegroundNotification(Context context) {
    String raw = PushySingleton.getSettings(context).getString(PushyPreferenceKeys.FOREGROUND_NOTIFICATION, null);
    if (raw == null)
      return null; 
    byte[] array = PushyParcelableUtil.stringToByteArray(raw);
    Notification notification = (Notification)PushyParcelableUtil.unmarshall(array, Notification.CREATOR);
    notification.contentIntent = PushyPushReceiver.getMainActivityPendingIntent(context);
    return notification;
  }
  
  public void onTaskRemoved(Intent rootIntent) {
    PushyLogger.d("Task removed, attempting restart in 3 seconds");
    Intent restartService = new Intent(getApplicationContext(), getClass());
    restartService.setPackage(getPackageName());
    PendingIntent restartServiceIntent = PendingIntent.getService(getApplicationContext(), 1, restartService, 1073741824);
    this.mAlarmManager.set(3, SystemClock.elapsedRealtime() + 3000L, restartServiceIntent);
  }
  
  private void handleCrashedService() {
    stopKeepAliveTimerAndWifiLock();
    cancelReconnect();
  }
  
  public void onDestroy() {
    PushyLogger.d("Service destroyed");
    this.mIsDestroyed = true;
    stop();
    (new StopForegroundAsync()).execute(new Integer[0]);
    super.onDestroy();
  }
  
  public int onStartCommand(Intent intent, int flags, int startId) {
    if (intent == null || intent.getAction() == null) {
      start();
    } else if (intent.getAction().equals(ACTION_START)) {
      start();
    } else if (intent.getAction().equals(ACTION_KEEP_ALIVE)) {
      sendKeepAlive();
    } else if (intent.getAction().equals(ACTION_RECONNECT)) {
      reconnectAsync();
    } 
    return 1;
  }
  
  private void start() {
    if (!PushyNetworking.isNetworkAvailable(this.mConnectivityManager))
      return; 
    if (mSocket.isConnecting() || mSocket.isConnected())
      return; 
    (new ConnectAsync()).execute(new Integer[0]);
  }
  
  private void stop() {
    cancelReconnect();
    stopKeepAliveTimerAndWifiLock();
    unregisterReceiver(this.mConnectivityListener);
    mSocket.disconnectExistingClientAsync();
  }
  
  private void sendKeepAlive() {
    if (mSocket.isConnected())
      (new SendKeepAliveAsync()).execute(new Integer[0]); 
  }
  
  private void startKeepAliveTimerAndWifiLock() {
    long interval = (PushyMqttConnection.getKeepAliveInterval((Context)this) * 1000);
    PendingIntent pendingIntent = getAlarmPendingIntent(ACTION_KEEP_ALIVE);
    this.mAlarmManager.setRepeating(0, System.currentTimeMillis() + interval, interval, pendingIntent);
    mSocket.acquireWifiLock();
  }
  
  PendingIntent getAlarmPendingIntent(String action) {
    Intent keepAliveIntent = new Intent();
    keepAliveIntent.setClass((Context)this, PushySocketService.class);
    keepAliveIntent.setAction(action);
    return PendingIntent.getService((Context)this, 0, keepAliveIntent, 0);
  }
  
  private void stopKeepAliveTimerAndWifiLock() {
    PendingIntent keepAliveIntent = getAlarmPendingIntent(ACTION_KEEP_ALIVE);
    this.mAlarmManager.cancel(keepAliveIntent);
    mSocket.releaseWifiLock();
  }
  
  public void scheduleReconnect() {
    long now = System.currentTimeMillis();
    if (this.mRetryInterval < PushyMQTT.MAXIMUM_RETRY_INTERVAL)
      this.mRetryInterval = Math.min(this.mRetryInterval * 2L, PushyMQTT.MAXIMUM_RETRY_INTERVAL);
    PushyLogger.d("Reconnecting in " + this.mRetryInterval + "ms.");
    PendingIntent keepAliveIntent = getAlarmPendingIntent(ACTION_RECONNECT);
    this.mAlarmManager.set(0, now + this.mRetryInterval, keepAliveIntent);
  }
  
  public void cancelReconnect() {
    PendingIntent reconnectIntent = getAlarmPendingIntent(ACTION_RECONNECT);
    this.mAlarmManager.cancel(reconnectIntent);
  }
  
  private void reconnectAsync() {
    stopKeepAliveTimerAndWifiLock();
    if (this.mIsDestroyed) {
      PushyLogger.d("Not reconnecting (service destroyed)");
      return;
    } 
    if (!PushyNetworking.isNetworkAvailable(this.mConnectivityManager)) {
      PushyLogger.d("Not reconnecting (network not available)");
      return;
    } 
    if (mSocket.isConnecting()) {
      PushyLogger.d("Already reconnecting");
      return;
    } 
    PushyLogger.d("Reconnecting...");
    (new ConnectAsync()).execute(new Integer[0]);
  }
  
  private BroadcastReceiver mConnectivityListener = new BroadcastReceiver() {
      public void onReceive(Context context, Intent intent) {
        if (PushyNetworking.isNetworkAvailable(PushySocketService.this.mConnectivityManager)) {
          PushyLogger.d("Internet connected");
          if (!PushySocketService.mSocket.isConnected()) {
            PushySocketService.this.reconnectAsync();
          } else if (PushyNetworking.getConnectedNetwork(PushySocketService.this.mConnectivityManager) == 1 && PushySocketService.mSocket.getNetwork() == 0) {
            PushySocketService.this.reconnectAsync();
          } 
        } else {
          PushyLogger.d("Internet disconnected");
          PushySocketService.this.cancelReconnect();
        } 
      }
    };
  
  public IBinder onBind(Intent intent) {
    return null;
  }
  
  public class ConnectAsync extends AsyncTask<Integer, String, Integer> {
    public ConnectAsync() {
      PushySocketService.mSocket.setConnecting(true);
    }
    
    protected Integer doInBackground(Integer... parameter) {
      if (!PushyPreferences.getBoolean(PushyPreferenceKeys.NOTIFICATIONS_ENABLED, true, PushySocketService.this.getApplicationContext())) {
        PushyLogger.d("Notifications have been disabled by the app");
        PushySocketService.this.cancelReconnect();
        PushySocketService.this.stopSelf();
        return Integer.valueOf(0);
      } 
      PushyLogger.d("Connecting...");
      try {
        PushySocketService.mSocket.connect();
        if (PushySocketService.this.mIsDestroyed) {
          PushySocketService.mSocket.disconnectExistingClientSync();
          PushyLogger.d("Service destroyed, aborting connection");
          return Integer.valueOf(0);
        } 
        PushySocketService.this.mRetryInterval = PushyMQTT.INITIAL_RETRY_INTERVAL;
        PushySocketService.this.startKeepAliveTimerAndWifiLock();
        PushyLogger.d("Connected successfully (sending keep alive every " + PushyMqttConnection.getKeepAliveInterval((Context)PushySocketService.this) + " seconds)");
      } catch (Exception e) {
        PushyLogger.d("Connect exception: " + e.toString(), e);
        if (e.getClass() == PushyFatalException.class) {
          PushyLogger.d("Fatal error encountered, stopping service");
          PushySocketService.this.stopSelf();
          return Integer.valueOf(0);
        } 
        if (e.getClass() == MqttSecurityException.class)
          if (((MqttSecurityException)e).getReasonCode() == 5) {
            PushyLogger.d("MQTT connect returned error code 5, clearing the device credentials");
            PushyAuthentication.clearDeviceCredentials((Context)PushySocketService.this);
            PushySocketService.this.stopSelf();
            return Integer.valueOf(0);
          }  
        if (PushyNetworking.isNetworkAvailable(PushySocketService.this.mConnectivityManager))
          PushySocketService.this.scheduleReconnect(); 
      } finally {
        PushySocketService.mSocket.setConnecting(false);
      } 
      return Integer.valueOf(0);
    }
  }
  
  public class SendKeepAliveAsync extends AsyncTask<Integer, String, Integer> {
    protected Integer doInBackground(Integer... parameter) {
      if (!PushySocketService.mSocket.isConnected())
        return Integer.valueOf(0); 
      PushyLogger.d("Sending keep alive");
      try {
        PushySocketService.mSocket.sendKeepAlive();
      } catch (Exception e) {
        PushyLogger.d("Keep alive error: " + e.toString(), e);
        PushySocketService.mSocket.disconnectExistingClientSync();
        PushySocketService.this.reconnectAsync();
      } 
      return Integer.valueOf(0);
    }
  }
  
  public class StartForegroundAsync extends AsyncTask<Integer, String, Integer> {
    protected Integer doInBackground(Integer... parameter) {
      if (PushySocketService.isForegroundServiceEnabled((Context)PushySocketService.this))
        PushySocketService.this.startForeground(PushyMQTT.FOREGROUND_NOTIFICATION_ID, PushySocketService.getForegroundNotification((Context)PushySocketService.this));
      return Integer.valueOf(0);
    }
  }
  
  public class StopForegroundAsync extends AsyncTask<Integer, String, Integer> {
    protected Integer doInBackground(Integer... parameter) {
      if (PushySocketService.isForegroundServiceEnabled((Context)PushySocketService.this))
        PushySocketService.this.stopForeground(true); 
      return Integer.valueOf(0);
    }
  }
}
