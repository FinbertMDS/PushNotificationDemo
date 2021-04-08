package com.pushnotification.util;

public class PushyStringUtils {
  public static boolean stringIsNullOrEmpty(String input) {
    if (input == null)
      return true; 
    if (input.trim().equals(""))
      return true; 
    return false;
  }
  
  public static boolean equals(String a, String b) {
    if (a == null && b == null)
      return true; 
    if (a == null)
      return false; 
    return a.equals(b);
  }
}
