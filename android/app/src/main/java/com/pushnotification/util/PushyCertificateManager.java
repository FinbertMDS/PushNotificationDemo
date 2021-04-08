package com.pushnotification.util;

import android.content.Context;

import com.pushnotification.config.PushyPreferenceKeys;

import java.io.InputStream;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import javax.net.SocketFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

public class PushyCertificateManager {
  public static SocketFactory mSocketFactory;
  
  public static boolean isConfigured(Context context) {
    return (getEnterpriseCertificateName(context) != null);
  }
  
  public static String getEnterpriseCertificateName(Context context) {
    return PushyPreferences.getString(PushyPreferenceKeys.ENTERPRISE_CERTIFICATE, null, context);
  }
  
  public static SocketFactory getEnterpriseSslSocketFactory(Context context) {
    if (mSocketFactory != null)
      return mSocketFactory; 
    try {
      CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
      InputStream certificateFile = context.getResources().openRawResource(context.getResources().getIdentifier(getEnterpriseCertificateName(context), "raw", context.getPackageName()));
      Certificate certificate = certificateFactory.generateCertificate(certificateFile);
      KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
      keyStore.load(null, null);
      keyStore.setCertificateEntry("ca", certificate);
      TrustManagerFactory trustManager = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
      trustManager.init(keyStore);
      SSLContext sslContext = SSLContext.getInstance("TLS");
      sslContext.init(null, trustManager.getTrustManagers(), null);
      mSocketFactory = sslContext.getSocketFactory();
      PushyLogger.d("Enterprise certificate loaded successfully");
    } catch (Exception exc) {
      PushyLogger.e("Enterprise certificate configuration failed", exc);
    } 
    return mSocketFactory;
  }
}
