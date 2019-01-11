
package com.simplegeofencing.reactnative;

import android.app.PendingIntent;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.util.Log;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import java.util.ArrayList;
import java.util.List;

public class RNSimpleNativeGeofencingModule extends ReactContextBaseJavaModule {
  private GeofencingClient mGeofencingClient;
  private List<Geofence> mGeofenceList;
  private Geofence mMonitorGeofence;
  private Callback mMonitorCallback;
  private PendingIntent mGeofencePendingIntent;
  private PendingIntent mMonitorGeofencePendingIntent;
  private final ReactApplicationContext reactContext;
  private final String TAG = "SNGeofencing";

  public RNSimpleNativeGeofencingModule(ReactApplicationContext reactContext) {
    super(reactContext);
    this.reactContext = reactContext;
    mGeofencingClient = LocationServices.getGeofencingClient(reactContext);
    mGeofenceList = new ArrayList<Geofence>();
  }

  @Override
  public String getName() {
    return "RNSimpleNativeGeofencing";
  }

  /*
    React Native functions
   */

  @ReactMethod
  public void removeAllGeofences(){
    mGeofenceList.clear();
    stopMonitoring();
  }

  @ReactMethod
  public void removeGeofence(String key){
    int index = -1;
    for (int i = 0; i < mGeofenceList.size(); i++){
      if(mGeofenceList.get(i).getRequestId() == key){
        index = i;
      }
    }
    mGeofenceList.remove(index);
  }

  @ReactMethod
  public void addMonitoringBorder(ReadableMap monitorGeofenceObject, int duration, Callback callback){
    mMonitorGeofence = new Geofence.Builder()
      .setRequestId(monitorGeofenceObject.getString("key"))
      .setCircularRegion(
        monitorGeofenceObject.getDouble("latitude"),
        monitorGeofenceObject.getDouble("longitude"),
        monitorGeofenceObject.getInt("radius")
      )
      .setExpirationDuration(duration)
      .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER |
        Geofence.GEOFENCE_TRANSITION_EXIT)
      .build();
    mMonitorCallback = callback;
  }
  @ReactMethod
  public void removeMonitoringBorder(){
    mMonitorGeofence = null;
    mMonitorCallback = null;
    stopMonitoring();
  }

  @ReactMethod
  public void addGeofences(
          ReadableArray geofenceArray,
          ReadableMap monitoringGeofence,
          int duration,
          Callback monitoringCallback)
  {
    //Add monitor geofence
    addMonitoringBorder(monitoringGeofence, duration, monitoringCallback);
    //Add geohashes
    for (int i = 0; i < geofenceArray.size(); i++) {
      ReadableMap geofence = geofenceArray.getMap(i);
      addGeofence(geofence, duration);
    }
    //Start Monitoring
    startMonitoring();
  }

  @ReactMethod
  public void addGeofence(ReadableMap geofenceObject, int duration) {
    mGeofenceList.add(new Geofence.Builder()
      .setRequestId(geofenceObject.getString("key"))
      .setCircularRegion(
        geofenceObject.getDouble("latitude"),
        geofenceObject.getDouble("longitude"),
        geofenceObject.getInt("radius")
      )
      .setExpirationDuration(duration)
      .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER |
        Geofence.GEOFENCE_TRANSITION_EXIT)
      .build());
  }

  @ReactMethod
  public void startMonitoring() {
    //Context removed by Listeners
    mGeofencingClient.addGeofences(getGeofencingRequest(), getGeofencePendingIntent())
      .addOnSuccessListener(new OnSuccessListener<Void>() {
        @Override
        public void onSuccess(Void aVoid) {
          Log.i(TAG, "Start Monitoring");
        }
      })
      .addOnFailureListener(new OnFailureListener() {
        @Override
        public void onFailure(@NonNull Exception e) {
            Log.e(TAG, "Start Monitoring: " + e.getMessage());
        }
      });
  }

  @ReactMethod
  public void stopMonitoring() {
    //Context removed by Listeners
    mGeofencingClient.removeGeofences(getGeofencePendingIntent())
      .addOnSuccessListener(new OnSuccessListener<Void>() {
        @Override
        public void onSuccess(Void aVoid) {
          Log.i(TAG, "Stop Monitoring");
        }
      })
      .addOnFailureListener(new OnFailureListener() {
        @Override
        public void onFailure(@NonNull Exception e) {
          Log.e(TAG, "Stop Monitoring: " + e.getMessage());
        }
      });
  }

  /*
    Helpfunctions
   */

  private GeofencingRequest getGeofencingRequest() {
    GeofencingRequest.Builder builder = new GeofencingRequest.Builder();
    builder.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER);
    builder.addGeofences(mGeofenceList);
    return builder.build();
  }

  private PendingIntent getGeofencePendingIntent() {
    // Reuse the PendingIntent if we already have it.
    if (mGeofencePendingIntent != null) {
      return mGeofencePendingIntent;
    }
    Intent intent = new Intent(this.reactContext, GeofenceTransitionsIntentService.class);
    // We use FLAG_UPDATE_CURRENT so that we get the same pending intent back when
    // calling addGeofences() and removeGeofences().
    mGeofencePendingIntent = PendingIntent.getService(this.reactContext, 0, intent, PendingIntent.
            FLAG_UPDATE_CURRENT);
    return mGeofencePendingIntent;
  }

  private GeofencingRequest getMonitorGeofencingRequest() {
    GeofencingRequest.Builder builder = new GeofencingRequest.Builder();
    builder.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_EXIT);
    builder.addGeofence(mMonitorGeofence);
    return builder.build();
  }

  private PendingIntent getMonitorGeofencePendingIntent() {
    // Reuse the PendingIntent if we already have it.
    if (mMonitorGeofencePendingIntent != null) {
      return mMonitorGeofencePendingIntent;
    }
    Intent intent = new Intent(this.reactContext, MonitorTransitionsIntentService.class);
    // We use FLAG_UPDATE_CURRENT so that we get the same pending intent back when
    // calling addGeofences() and removeGeofences().
    mMonitorGeofencePendingIntent = PendingIntent.getService(this.reactContext, 0, intent, PendingIntent.
            FLAG_UPDATE_CURRENT);
    return mMonitorGeofencePendingIntent;
  }
}