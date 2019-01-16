
package com.simplegeofencing.reactnative;

import android.app.NotificationManager;
import android.app.NotificationChannel;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Build;
import android.support.annotation.NonNull;
//import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
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
  private Geofence mMonitorGeofence=null;
  private Callback mMonitorCallback=null;
  private PendingIntent mGeofencePendingIntent;
  private PendingIntent mMonitorGeofencePendingIntent;
  private final ReactApplicationContext reactContext;
  private final String TAG = "SNGeofencing";
  private final String CHANNEL_ID = "channel_01";
  private int notificationId = 1;
  private NotificationChannel channel;
  public String[] notifyChannelString = new String[2];
  private boolean notifyStart = false;
  private boolean notifyStop = false;
  public boolean notifyEnter = false;
  public boolean notifyExit = false;
  public String[] notifyStartString = new String[2];
  public String[] notifyStopString = new String[2];
  public String[] notifyEnterString = new String[2];
  public String[] notifyExitString = new String[2];


  public RNSimpleNativeGeofencingModule(ReactApplicationContext reactContext) {
    super(reactContext);
    this.reactContext = reactContext;
    mGeofencingClient = LocationServices.getGeofencingClient(reactContext);
    mGeofenceList = new ArrayList<Geofence>();
    notifyChannelString[0] = "Title";
    notifyChannelString[1] = "Description";
  }

  @Override
  public String getName() {
    return "RNSimpleNativeGeofencing";
  }

  /*
    React Native functions
   */

  @ReactMethod
  public void initNotification(ReadableMap pText){
    ReadableMap pChannel = pText.getMap("channel");
    notifyChannelString[0] = pChannel.getString("title");
    notifyChannelString[1] = pChannel.getString("description");
    ReadableMap pStart = pText.getMap("start");
    if(pStart.getBoolean("notify")){
      notifyStart = true;
      notifyStartString[0] = pStart.getString("title");
      notifyStartString[1] = pStart.getString("description");
    }
    ReadableMap pStop = pText.getMap("stop");
    if(pStop.getBoolean("notify")){
      notifyStop = true;
      notifyStopString[0] = pStop.getString("title");
      notifyStopString[1] = pStop.getString("description");
    }
    ReadableMap pEnter = pText.getMap("enter");
    if(pEnter.getBoolean("notify")){
      notifyEnter = true;
      notifyEnterString[0] = pEnter.getString("title");
      notifyEnterString[1] = pEnter.getString("description");
    }
    ReadableMap pExit = pText.getMap("exit");
    if(pExit.getBoolean("notify")){
      notifyExit = true;
      notifyExitString[0] = pExit.getString("title");
      notifyExitString[1] = pExit.getString("description");
    }
  }

  @ReactMethod
  public void removeAllGeofences(){
    mGeofenceList.clear();
    removeMonitoringBorder();
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
    if(index != -1){
      mGeofenceList.remove(index);
      //Remove from Client as well
      List<String> item = new ArrayList<String>();
      item.add(key);
      mGeofencingClient.removeGeofences(item);
    }
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
  public void updateGeofences(
          ReadableArray geofenceArray,
          ReadableMap monitoringGeofence,
          int duration,
          Callback monitoringCallback
  ){
    mGeofenceList.clear();
    silentStopMonitoring();
    //Add monitor geofence
    addMonitoringBorder(monitoringGeofence, duration, monitoringCallback);
    //Add geohashes
    for (int i = 0; i < geofenceArray.size(); i++) {
      ReadableMap geofence = geofenceArray.getMap(i);
      addGeofence(geofence, duration);
    }
    silentStartMonitoring();
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
            Log.i(TAG, "Added Geofences");
            if(mMonitorGeofence != null){
              mGeofencingClient.addGeofences(
                      getMonitorGeofencingRequest(),
                      getMonitorGeofencePendingIntent()
              ).addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                  Log.i(TAG, "Added Monitoring Geofence");
                  notifyNow("start");
                }
              }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                  Log.e(TAG, "Adding Monitoring Geofence: " + e.getMessage());
                }
              });
            }else{
              notifyNow("start");
            }
          }
        })
        .addOnFailureListener(new OnFailureListener() {
          @Override
          public void onFailure(@NonNull Exception e) {
            Log.e(TAG, "Adding Geofences: " + e.getMessage());
          }
        });
    //}

  }
  public void silentStartMonitoring() {
    //Context removed by Listeners
    //if (ContextCompat.checkSelfPermission(this.reactContext, Manifest.permission.ACCESS_FINE_LOCATION)
    //        != PackageManager.PERMISSION_GRANTED){
    mGeofencingClient.addGeofences(getGeofencingRequest(), getGeofencePendingIntent())
            .addOnSuccessListener(new OnSuccessListener<Void>() {
              @Override
              public void onSuccess(Void aVoid) {
                Log.i(TAG, "Updated Geofences");
                if(mMonitorGeofence != null){
                  mGeofencingClient.addGeofences(
                          getMonitorGeofencingRequest(),
                          getMonitorGeofencePendingIntent()
                  ).addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                      Log.i(TAG, "Updated Monitoring Geofence");
                    }
                  }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                      Log.e(TAG, "Updating Monitoring Geofence: " + e.getMessage());
                    }
                  });
                }
              }
            })
            .addOnFailureListener(new OnFailureListener() {
              @Override
              public void onFailure(@NonNull Exception e) {
                Log.e(TAG, "Updating Geofences: " + e.getMessage());
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
          Log.i(TAG, "Removed Geofences");
          if(mMonitorGeofence != null) {
            mGeofencingClient.removeGeofences(getMonitorGeofencePendingIntent())
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                      @Override
                      public void onSuccess(Void aVoid) {
                        Log.i(TAG, "Removed Monitoring Geofence");
                        notifyNow("stop");
                      }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                      @Override
                      public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "Removing Monitoring Geofences: " + e.getMessage());
                      }
                    });
          }else{
            notifyNow("stop");
          }
        }
      })
      .addOnFailureListener(new OnFailureListener() {
        @Override
        public void onFailure(@NonNull Exception e) {
          Log.e(TAG, "Removing Geofences: " + e.getMessage());
        }
      });
  }

  public void silentStopMonitoring() {
    //Context removed by Listeners
    mGeofencingClient.removeGeofences(getGeofencePendingIntent())
            .addOnSuccessListener(new OnSuccessListener<Void>() {
              @Override
              public void onSuccess(Void aVoid) {
                Log.i(TAG, "Removed Geofences");
                if(mMonitorGeofence != null) {
                  mGeofencingClient.removeGeofences(getMonitorGeofencePendingIntent())
                          .addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                              Log.i(TAG, "Removed Monitoring Geofence");
                            }
                          })
                          .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                              Log.e(TAG, "Removing Monitoring Geofences: " + e.getMessage());
                            }
                          });
                }
              }
            })
            .addOnFailureListener(new OnFailureListener() {
              @Override
              public void onFailure(@NonNull Exception e) {
                Log.e(TAG, "Removing Geofences: " + e.getMessage());
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
    Intent intent = new Intent(reactContext, GeofenceTransitionsIntentService.class);
    // Add notification data
    intent.putExtra("notifyEnter", notifyEnter);
    if(notifyEnter == true){
      intent.putExtra("notifyEnterStringTitle", notifyEnterString[0]);
      intent.putExtra("notifyEnterStringDescription", notifyEnterString[1]);
    }else{
      intent.putExtra("notifyEnterStringTitle", "");
      intent.putExtra("notifyEnterStringDescription", "");
    }
    intent.putExtra("notifyExit", notifyExit);
    if(notifyExit == true){
      intent.putExtra("notifyExitStringTitle", notifyExitString[0]);
      intent.putExtra("notifyExitStringDescription", notifyExitString[1]);
    }else{
      intent.putExtra("notifyExitStringTitle", "");
      intent.putExtra("notifyExitStringDescription", "");
    }
    intent.putExtra("notifyChannelStringTitle", notifyChannelString[0]);
    intent.putExtra("notifyChannelStringDescription", notifyChannelString[1]);
    // We use FLAG_UPDATE_CURRENT so that we get the same pending intent back when
    // calling addGeofences() and removeGeofences().
    mGeofencePendingIntent = PendingIntent.getService(reactContext, 0, intent, PendingIntent.
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
  private void notifyNow(String action){
    if(action == "start"){
      if(notifyStart == true){
        postNotification(notifyStartString[0], notifyStartString[1]);
      }
    }
    if(action == "stop"){
      if(notifyStop == true){
        postNotification(notifyStopString[0], notifyStopString[1]);
      }
    }
  }
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
      CharSequence name = notifyChannelString[0];
      String description = notifyChannelString[1];
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