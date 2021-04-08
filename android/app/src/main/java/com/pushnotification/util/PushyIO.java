package com.pushnotification.util;

import android.content.Context;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

public class PushyIO {
  public static String readFromFile(String path, Context context) throws Exception {
    String contents = null;
    File file = new File(path);
    if (file != null) {
      BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
      contents = bufferedReader.readLine();
      bufferedReader.close();
    } 
    return contents;
  }
  
  public static boolean fileExists(String path) {
    File file = new File(path);
    return file.exists();
  }
}
