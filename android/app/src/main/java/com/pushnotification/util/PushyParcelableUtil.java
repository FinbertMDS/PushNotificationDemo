package com.pushnotification.util;

import android.os.Parcel;
import android.os.Parcelable;
import java.util.Arrays;

public class PushyParcelableUtil {
  public static byte[] marshall(Parcelable parceable) {
    Parcel parcel = Parcel.obtain();
    parceable.writeToParcel(parcel, 0);
    byte[] bytes = parcel.marshall();
    parcel.recycle();
    return bytes;
  }
  
  public static Parcel unmarshall(byte[] bytes) {
    Parcel parcel = Parcel.obtain();
    parcel.unmarshall(bytes, 0, bytes.length);
    parcel.setDataPosition(0);
    return parcel;
  }
  
  public static <T> T unmarshall(byte[] bytes, Parcelable.Creator<T> creator) {
    Parcel parcel = unmarshall(bytes);
    T result = (T)creator.createFromParcel(parcel);
    parcel.recycle();
    return result;
  }
  
  public static byte[] stringToByteArray(String string) {
    String[] split = string.substring(1, string.length() - 1).split(", ");
    byte[] array = new byte[split.length];
    for (int i = 0; i < split.length; i++)
      array[i] = Byte.parseByte(split[i]); 
    return array;
  }
  
  public static String byteArrayToString(byte[] array) {
    return Arrays.toString(array);
  }
}
