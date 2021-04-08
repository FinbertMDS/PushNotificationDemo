package com.pushnotification.react.util;

import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.ReadableMapKeySetIterator;
import com.facebook.react.bridge.ReadableType;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableNativeArray;
import com.facebook.react.bridge.WritableNativeMap;
import java.util.Iterator;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class PushyMapUtils {
  public static WritableMap convertJsonToMap(JSONObject jsonObject) throws JSONException {
    WritableNativeMap writableNativeMap = new WritableNativeMap();
    Iterator<String> iterator = jsonObject.keys();
    while (iterator.hasNext()) {
      String key = iterator.next();
      Object value = jsonObject.get(key);
      if (value instanceof JSONObject) {
        writableNativeMap.putMap(key, convertJsonToMap((JSONObject)value));
        continue;
      } 
      if (value instanceof JSONArray) {
        writableNativeMap.putArray(key, convertJsonToArray((JSONArray)value));
        continue;
      } 
      if (value instanceof Boolean) {
        writableNativeMap.putBoolean(key, ((Boolean)value).booleanValue());
        continue;
      } 
      if (value instanceof Integer) {
        writableNativeMap.putInt(key, ((Integer)value).intValue());
        continue;
      } 
      if (value instanceof Double) {
        writableNativeMap.putDouble(key, ((Double)value).doubleValue());
        continue;
      } 
      if (value instanceof String) {
        writableNativeMap.putString(key, (String)value);
        continue;
      } 
      writableNativeMap.putString(key, value.toString());
    } 
    return (WritableMap)writableNativeMap;
  }
  
  public static WritableArray convertJsonToArray(JSONArray jsonArray) throws JSONException {
    WritableNativeArray writableNativeArray = new WritableNativeArray();
    for (int i = 0; i < jsonArray.length(); i++) {
      Object value = jsonArray.get(i);
      if (value instanceof JSONObject) {
        writableNativeArray.pushMap(convertJsonToMap((JSONObject)value));
      } else if (value instanceof JSONArray) {
        writableNativeArray.pushArray(convertJsonToArray((JSONArray)value));
      } else if (value instanceof Boolean) {
        writableNativeArray.pushBoolean(((Boolean)value).booleanValue());
      } else if (value instanceof Integer) {
        writableNativeArray.pushInt(((Integer)value).intValue());
      } else if (value instanceof Double) {
        writableNativeArray.pushDouble(((Double)value).doubleValue());
      } else if (value instanceof String) {
        writableNativeArray.pushString((String)value);
      } else {
        writableNativeArray.pushString(value.toString());
      } 
    } 
    return (WritableArray)writableNativeArray;
  }
  
  public static JSONObject convertMapToJson(ReadableMap readableMap) throws JSONException {
    JSONObject object = new JSONObject();
    ReadableMapKeySetIterator iterator = readableMap.keySetIterator();
    while (iterator.hasNextKey()) {
      String key = iterator.nextKey();
      switch (readableMap.getType(key)) {
        case Null:
          object.put(key, JSONObject.NULL);
        case Boolean:
          object.put(key, readableMap.getBoolean(key));
        case Number:
          object.put(key, readableMap.getDouble(key));
        case String:
          object.put(key, readableMap.getString(key));
        case Map:
          object.put(key, convertMapToJson(readableMap.getMap(key)));
        case Array:
          object.put(key, convertArrayToJson(readableMap.getArray(key)));
      } 
    } 
    return object;
  }
  
  public static JSONArray convertArrayToJson(ReadableArray readableArray) throws JSONException {
    JSONArray array = new JSONArray();
    for (int i = 0; i < readableArray.size(); i++) {
      switch (readableArray.getType(i)) {
        case Boolean:
          array.put(readableArray.getBoolean(i));
          break;
        case Number:
          array.put(readableArray.getDouble(i));
          break;
        case String:
          array.put(readableArray.getString(i));
          break;
        case Map:
          array.put(convertMapToJson(readableArray.getMap(i)));
          break;
        case Array:
          array.put(convertArrayToJson(readableArray.getArray(i)));
          break;
      } 
    } 
    return array;
  }
}
