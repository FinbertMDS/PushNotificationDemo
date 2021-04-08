package com.pushnotification.util;

import android.content.Context;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;

import com.pushnotification.config.PushyAPIConfig;
import com.pushnotification.util.exceptions.PushyNetworkException;

public class PushyHTTP {
  public static String get(String urlString, Context context) throws PushyNetworkException {
    HttpsURLConnection httpConnection = null;
    try {
      InputStream inputStream;
      URL url = new URL(urlString);
      httpConnection = (HttpsURLConnection)url.openConnection();
      if (PushyEnterprise.isConfigured(context) && PushyCertificateManager.isConfigured(context))
        httpConnection.setSSLSocketFactory((SSLSocketFactory)PushyCertificateManager.getEnterpriseSslSocketFactory(context)); 
      httpConnection.setUseCaches(false);
      httpConnection.setRequestMethod("GET");
      httpConnection.setReadTimeout(PushyAPIConfig.TIMEOUT);
      httpConnection.setConnectTimeout(PushyAPIConfig.TIMEOUT);
      httpConnection.connect();
      int statusCode = httpConnection.getResponseCode();
      if (statusCode == 200) {
        inputStream = httpConnection.getInputStream();
      } else {
        PushyLogger.e("Internal API request failed with status code " + statusCode);
        inputStream = httpConnection.getErrorStream();
      } 
      BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
      StringBuffer response = new StringBuffer();
      String tmpLine;
      while ((tmpLine = reader.readLine()) != null)
        response.append(tmpLine); 
      reader.close();
      return response.toString();
    } catch (Exception exc) {
      throw new PushyNetworkException(exc.toString() + exc.getStackTrace());
    } finally {
      if (httpConnection != null)
        httpConnection.disconnect(); 
    } 
  }
  
  public static String post(String urlString, String json, Context context) throws PushyNetworkException {
    HttpsURLConnection httpConnection = null;
    try {
      InputStream inputStream;
      URL url = new URL(urlString);
      httpConnection = (HttpsURLConnection)url.openConnection();
      if (PushyEnterprise.isConfigured(context) && PushyCertificateManager.isConfigured(context))
        httpConnection.setSSLSocketFactory((SSLSocketFactory)PushyCertificateManager.getEnterpriseSslSocketFactory(context)); 
      httpConnection.setRequestProperty("Accept", "application/json");
      httpConnection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
      httpConnection.setDoInput(true);
      httpConnection.setDoOutput(true);
      httpConnection.setUseCaches(false);
      httpConnection.setRequestMethod("POST");
      httpConnection.setReadTimeout(PushyAPIConfig.TIMEOUT);
      httpConnection.setConnectTimeout(PushyAPIConfig.TIMEOUT);
      OutputStream outputStream = httpConnection.getOutputStream();
      outputStream.write(json.getBytes("UTF-8"));
      outputStream.close();
      int statusCode = httpConnection.getResponseCode();
      if (statusCode == 200) {
        inputStream = httpConnection.getInputStream();
      } else {
        PushyLogger.e("Internal API request failed with status code " + statusCode);
        inputStream = httpConnection.getErrorStream();
      } 
      BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
      StringBuffer response = new StringBuffer();
      String tmpLine;
      while ((tmpLine = reader.readLine()) != null)
        response.append(tmpLine); 
      reader.close();
      return response.toString();
    } catch (Exception exc) {
      throw new PushyNetworkException(exc.toString() + exc.getStackTrace());
    } finally {
      if (httpConnection != null)
        httpConnection.disconnect(); 
    } 
  }
}
