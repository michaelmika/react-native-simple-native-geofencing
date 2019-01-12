
package com.simplegeofencing.reactnative;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.NotificationChannel;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.annotation.NonNull;
//import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.ContextCompat;
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
  private final String CHANNEL_ID = "channel_01";
  private int notificationId = 1;
  private NotificationChannel channel;

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
    Log.i(TAG, "Added geofence: Lat " + geofenceObject.getDouble("latitude") + " Long " + geofenceObject.getDouble("longitude"));
  }

  @ReactMethod
  public void startMonitoring() {
    //Context removed by Listeners
    //if (ContextCompat.checkSelfPermission(this.reactContext, Manifest.permission.ACCESS_FINE_LOCATION)
    //        != PackageManager.PERMISSION_GRANTED){
      mGeofencingClient.addGeofences(getGeofencingRequest(), getGeofencePendingIntent())
              .addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                  Log.i(TAG, "Start Monitoring");
                  postNotification("Start Monitoring", "Pressed Start Monitoring");
                }
              })
              .addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                  Log.e(TAG, "Start Monitoring: " + e.getMessage());
                }
              });
    //}

  }

  @ReactMethod
  public void stopMonitoring() {
    //Context removed by Listeners
    mGeofencingClient.removeGeofences(getGeofencePendingIntent())
      .addOnSuccessListener(new OnSuccessListener<Void>() {
        @Override
        public void onSuccess(Void aVoid) {
          Log.i(TAG, "Stop Monitoring");
          postNotification("Stop Monitoring", "Pressed Stop Monitoring");
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
    mGeofencePendingIntent = PendingIntent.getService(reactContext, 1, intent, PendingIntent.
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
    mMonitorGeofencePendingIntent = PendingIntent.getService(reactContext, 0, intent, PendingIntent.
            FLAG_UPDATE_CURRENT);
    return mMonitorGeofencePendingIntent;
  }
  /*
       Notifications
    */
  private NotificationCompat.Builder getNotificationBuilder(String Title, String Content) {
    //Onclick
    //Intent intent = new Intent(this.reactContext, AlertDetails.class);
    //intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
    //PendingIntent pendingIntent = PendingIntent.getActivity(this.reactContext, 0, intent, 0);

    //Build notification
    NotificationCompat.Builder notification = new NotificationCompat.Builder(this.reactContext, CHANNEL_ID)
            .setContentTitle(Title)
            .setContentText(Content)
            .setSmallIcon(getReactApplicationContext().getApplicationInfo().icon)
            .setAutoCancel(true);
    // Create the NotificationChannel, but only on API 26+ because
    // the NotificationChannel class is new and not in the support library
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && channel==null) {
      CharSequence name = "SenSafety";
      String description = "SenSafety Description";
      int importance = NotificationManager.IMPORTANCE_DEFAULT;
      NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
      channel.setDescription(description);
      // Register the channel with the system; you can't change the importance
      // or other notification behaviors after this
      NotificationManager notificationManager = this.reactContext.getSystemService(NotificationManager.class);
      notificationManager.createNotificationChannel(channel);
      notification.setChannelId(CHANNEL_ID);
    }
    return notification;
  }
  public void postNotification(String Title, String Content){
    NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this.reactContext);

    // notificationId is a unique int for each notification that you must define
    notificationManager.notify(notificationId, getNotificationBuilder(Title, Content).build());
    notificationId++;
  }

}