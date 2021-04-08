package com.pushnotification.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.provider.Settings;
import java.util.Map;
import javax.net.ssl.HostnameVerifier;

import com.pushnotification.config.PushyLogging;
import com.pushnotification.config.PushyMQTT;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttClientPersistence;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import com.pushnotification.config.PushyPreferenceKeys;
import com.pushnotification.model.PushyDeviceCredentials;
import com.pushnotification.util.exceptions.PushyFatalException;
import org.apache.http.conn.ssl.StrictHostnameVerifier;

public class PushyMqttConnection implements MqttCallback {
  int mNetwork;
  
  boolean mIsConnecting;
  
  Context mContext;
  
  MqttClient mClient;
  
  WifiManager mWifiManager;
  
  Runnable mConnectionLostRunnable;
  
  WifiManager.WifiLock mWifiWakeLock;
  
  ConnectivityManager mConnectivityManager;
  
  public PushyMqttConnection(Context context, WifiManager wifiManager, ConnectivityManager connectivityManager, Runnable connectionLostRunnable) {
    this.mContext = context;
    this.mWifiManager = wifiManager;
    this.mConnectivityManager = connectivityManager;
    this.mConnectionLostRunnable = connectionLostRunnable;
  }
  
  public static int getKeepAliveInterval(Context context) {
    return PushyPreferences.getInt(PushyPreferenceKeys.KEEPALIVE_INTERVAL, PushyMQTT.MQTT_DEFAULT_KEEP_ALIVE, context);
  }
  
  public void releaseWifiLock() {
    if (this.mWifiWakeLock == null)
      return; 
    try {
      this.mWifiWakeLock.release();
    } catch (Exception exc) {
      PushyLogger.d("Wi-Fi lock release failed");
    } 
    this.mWifiWakeLock = null;
    PushyLogger.d("Wi-Fi lock released");
  }
  
  public void acquireWifiLock() {
    if (this.mWifiWakeLock != null)
      return; 
    if (PushyPreferences.getBoolean(PushyPreferenceKeys.WIFI_POLICY_COMPLIANCE, false, this.mContext))
      if (Settings.System.getInt(this.mContext.getContentResolver(), "wifi_sleep_policy", 2) != 2) {
        PushyLogger.d("Complying with device Wi-Fi sleep policy");
        return;
      }  
    this.mWifiWakeLock = this.mWifiManager.createWifiLock(1, PushyLogging.TAG);
    this.mWifiWakeLock.acquire();
    PushyLogger.d("Wi-Fi lock acquired");
  }
  
  public void connect() throws Exception {
    disconnectExistingClientSync();
    String brokerEndpoint = PushyEndpoints.getMQTTEndpoint(this.mContext);
    PushyDeviceCredentials credentials = PushyAuthentication.getDeviceCredentials(this.mContext);
    if (credentials == null)
      throw new PushyFatalException("The device is not registered."); 
    PushyLogger.d("Broker: " + brokerEndpoint);
    PushyLogger.d("Device Token: " + credentials.token);
    PushyLogger.d("Device Auth Key: " + credentials.authKey.substring(0, 45) + "... [truncated]");
    this.mClient = new MqttClient(brokerEndpoint, credentials.token, (MqttClientPersistence)new MemoryPersistence());
    this.mClient.setCallback(this);
    this.mClient.setTimeToWait(PushyMQTT.MQTT_ACK_TIMEOUT);
    this.mNetwork = PushyNetworking.getConnectedNetwork(this.mConnectivityManager);
    MqttConnectOptions connectOptions = new MqttConnectOptions();
    connectOptions.setUserName(credentials.token);
    connectOptions.setPassword(credentials.authKey.toCharArray());
    connectOptions.setAutomaticReconnect(false);
    if (PushyEnterprise.isConfigured(this.mContext) && PushyCertificateManager.isConfigured(this.mContext))
      connectOptions.setSocketFactory(PushyCertificateManager.getEnterpriseSslSocketFactory(this.mContext)); 
    connectOptions.setSSLHostnameVerifier((HostnameVerifier)new StrictHostnameVerifier());
    connectOptions.setCleanSession(false);
    connectOptions.setConnectionTimeout(PushyMQTT.MQTT_CONNECT_TIMEOUT);
    connectOptions.setKeepAliveInterval(getKeepAliveInterval(this.mContext));
    this.mClient.connect(connectOptions);
    subscribeToTopic(this.mClient.getClientId());
  }
  
  private void subscribeToTopic(String topic) throws Exception {
    this.mClient.subscribe(topic, PushyMQTT.MQTT_QUALITY_OF_SERVICE);
  }
  
  private void publish(String topic, String payload) throws Exception {
    if (this.mClient == null || !this.mClient.isConnected())
      throw new Exception("Publish failed: not connected"); 
    this.mClient.publish(topic, payload.getBytes(), PushyMQTT.MQTT_QUALITY_OF_SERVICE, PushyMQTT.MQTT_RETAINED_PUBLISH);
  }
  
  public void sendKeepAlive() throws Exception {
    publish("keepalive", this.mClient.getClientId());
  }
  
  public void disconnectExistingClientAsync() {
    if (this.mClient == null || !this.mClient.isConnected())
      return; 
    AsyncTask.execute(new Runnable() {
          public void run() {
            PushyMqttConnection.this.disconnectExistingClientSync();
          }
        });
  }
  
  public void disconnectExistingClientSync() {
    if (this.mClient == null || !this.mClient.isConnected())
      return; 
    try {
      this.mClient.disconnectForcibly(2000L, 2000L);
      this.mClient.close();
    } catch (MqttException mqttException) {}
  }
  
  public boolean isConnected() {
    return (this.mClient != null && this.mClient.isConnected());
  }
  
  public void connectionLost(Throwable cause) {
    PushyLogger.d("Connection lost");
    if (this.mConnectionLostRunnable != null)
      this.mConnectionLostRunnable.run(); 
  }
  
  public void messageArrived(String topic, MqttMessage message) throws Exception {
    try {
      String json = new String(message.getPayload());
      Map<String, Object> payload = (Map<String, Object>)PushySingleton.getJackson().readValue(json, Map.class);
      PushyLogger.d("Received push for package " + this.mContext.getPackageName() + "\n" + payload);
      PushyBroadcastManager.publishNotification(this.mContext, payload, json);
    } catch (Exception exc) {
      PushyLogger.e("Publishing notification failed: " + exc.getMessage(), exc);
    } 
  }
  
  public void deliveryComplete(IMqttDeliveryToken token) {}
  
  public int getNetwork() {
    return this.mNetwork;
  }
  
  public boolean isConnecting() {
    return this.mIsConnecting;
  }
  
  public void setConnecting(boolean value) {
    this.mIsConnecting = value;
  }
}
