
package com.simplegeofencing.reactnative;

import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.NotificationChannel;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.preference.PreferenceManager;
import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import android.util.Log;

import com.facebook.react.HeadlessJsTaskService;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;


import java.util.ArrayList;
import java.util.List;

import static java.lang.Math.toIntExact;

public class RNSimpleNativeGeofencingModule extends ReactContextBaseJavaModule {
  private GeofencingClient mGeofencingClient;
  private List<Geofence> mGeofenceList;
  private PendingIntent mGeofencePendingIntent;
  private final ReactApplicationContext reactContext;
  private final String TAG = "SNGeofencing";
  private final String CHANNEL_ID = "channel_01";
  private NotificationChannel channel;
  private String[] notifyChannelString = new String[2];
  private boolean notifyStart = false;
  private boolean notifyStop = false;
  private boolean notifyEnter = false;
  private boolean notifyExit = false;
  private String[] notifyStartString = new String[2];
  private String[] notifyStopString = new String[2];
  private String[] notifyEnterString = new String[2];
  private String[] notifyExitString = new String[2];
  private Long mStartTime;
  private int mDuration;
  private LocalBroadcastReceiver mLocalBroadcastReceiver;
  private LocalBroadcastManager mLocalBroadcastManager;
  private static final String PREFERENCE_LAST_NOTIF_ID = "PREFERENCE_LAST_NOTIF_ID";
  private ArrayList<String> geofenceValues;
  private ArrayList<String> geofenceKeys;
  private static final String NOTIFICATION_TAG = "GeofenceNotification";
  private static final int NOTIFICATION_ID_START = 1;
  private static final int NOTIFICATION_ID_STOP = 150;


  public RNSimpleNativeGeofencingModule(ReactApplicationContext reactContext) {
    super(reactContext);
    this.reactContext = reactContext;
    this.mGeofencingClient = LocationServices.getGeofencingClient(reactContext);
    this.mGeofenceList = new ArrayList<Geofence>();
    this.mLocalBroadcastReceiver = new LocalBroadcastReceiver();
    this.mLocalBroadcastManager = LocalBroadcastManager.getInstance(reactContext);
    this.notifyChannelString[0] = "Title";
    this.notifyChannelString[1] = "Description";
    this.geofenceKeys = new ArrayList<String>();
    this.geofenceValues = new ArrayList<String>();
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
  public void removeAllGeofences(final Callback successCallback){
    if (mGeofenceList.size() === 0) {
      successCallback.invoke();
    }
    mGeofenceList.clear();
    stopMonitoring(successCallback);
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
  public void addGeofences(
          ReadableArray geofenceArray,
          int duration,
          Callback failCallback)
  {
    //Add geohashes
    for (int i = 0; i < geofenceArray.size(); i++) {
      ReadableMap geofence = geofenceArray.getMap(i);
      addGeofence(geofence, duration);
    }
    //Start Monitoring
    startMonitoring(failCallback);
  }

  @ReactMethod
  public void updateGeofences(
          ReadableArray geofenceArray,
          int duration
  ){
    mGeofenceList.clear();
    silentStopMonitoring();
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

    if(geofenceObject.hasKey("value")){
      geofenceKeys.add(geofenceObject.getString("key"));
      geofenceValues.add(geofenceObject.getString("value"));
    }
    mDuration = duration;
    Log.i(TAG, "Added geofence: Lat " + geofenceObject.getDouble("latitude") + " Long " + geofenceObject.getDouble("longitude"));
  }

  @ReactMethod
  public void startMonitoring(final Callback failCallback) {
    //Context removed by Listeners
    //if (ContextCompat.checkSelfPermission(this.reactContext, Manifest.permission.ACCESS_FINE_LOCATION)
    //        != PackageManager.PERMISSION_GRANTED){
      mGeofencingClient.addGeofences(getGeofencingRequest(), getGeofencePendingIntent())
        .addOnSuccessListener(new OnSuccessListener<Void>() {
          @Override
          public void onSuccess(Void aVoid) {
            Log.i(TAG, "Added Geofences");
            notifyNow("start");
            mStartTime = System.currentTimeMillis();
            mLocalBroadcastManager.registerReceiver(
                    mLocalBroadcastReceiver, new IntentFilter("outOfMonitorGeofence"));

            //Launch service to notify after timeout
            if(notifyStop == true){
              Intent notificationIntent = new Intent(reactContext, ShowTimeoutNotification.class);
              notificationIntent.putExtra("notifyChannelStringTitle", notifyChannelString[0]);
              notificationIntent.putExtra("notifyChannelStringDescription", notifyChannelString[1]);
              notificationIntent.putExtra("notifyStringTitle", notifyStopString[0]);
              notificationIntent.putExtra("notifyStringDescription", notifyStopString[1]);

              PendingIntent contentIntent = PendingIntent.getService(reactContext, 0, notificationIntent,
                      PendingIntent.FLAG_CANCEL_CURRENT);

              AlarmManager am = (AlarmManager) reactContext.getSystemService(Context.ALARM_SERVICE);
              am.cancel(contentIntent);
              if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, System.currentTimeMillis()+mDuration, contentIntent);
              }else{
                am.setExact(AlarmManager.RTC_WAKEUP, System.currentTimeMillis()+mDuration, contentIntent);
              }

            }

          }
        })
        .addOnFailureListener(new OnFailureListener() {
          @Override
          public void onFailure(@NonNull Exception e) {
            failCallback.invoke(e.getMessage());
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
  public void stopMonitoring(final Callback successCallback) {
    //Context removed by Listeners
    mGeofencingClient.removeGeofences(getGeofencePendingIntent())
      .addOnSuccessListener(new OnSuccessListener<Void>() {
        @Override
        public void onSuccess(Void aVoid) {
          Log.i(TAG, "Removed Geofences");

          notifyNow("stop");
          mLocalBroadcastManager.unregisterReceiver(mLocalBroadcastReceiver);
          if(notifyStop == true){
            Intent notificationIntent = new Intent(reactContext, ShowTimeoutNotification.class);
            notificationIntent.putExtra("notifyChannelStringTitle", notifyChannelString[0]);
            notificationIntent.putExtra("notifyChannelStringDescription", notifyChannelString[1]);
            notificationIntent.putExtra("notifyStringTitle", notifyStopString[0]);
            notificationIntent.putExtra("notifyStringDescription", notifyStopString[1]);

            PendingIntent contentIntent = PendingIntent.getService(reactContext, 0, notificationIntent,
                    PendingIntent.FLAG_CANCEL_CURRENT);

            AlarmManager am = (AlarmManager) reactContext.getSystemService(Context.ALARM_SERVICE);
            am.cancel(contentIntent);
          }
          successCallback.invoke();
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
              }
            })
            .addOnFailureListener(new OnFailureListener() {
              @Override
              public void onFailure(@NonNull Exception e) {
                Log.e(TAG, "Removing Geofences: " + e.getMessage());
              }
            });
  }

  @ReactMethod
  public void testNotify(){
    Log.i(TAG, "TestNotify Callback worked");
    postNotification("TestNotify", "Callback worked", false);
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
    Intent intent = new Intent(this.getCurrentActivity(), GeofenceTransitionsBroadcastReceiver.class);
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
    intent.putExtra("startTime", (Long) System.currentTimeMillis());
    intent.putExtra("duration", mDuration);
    intent.putStringArrayListExtra("geofenceKeys", geofenceKeys);
    intent.putStringArrayListExtra("geofenceValues", geofenceValues);
    // We use FLAG_UPDATE_CURRENT so that we get the same pending intent back when
    // calling addGeofences() and removeGeofences().
    mGeofencePendingIntent = PendingIntent.getBroadcast(reactContext, 0, intent, PendingIntent.
            FLAG_UPDATE_CURRENT);
    return mGeofencePendingIntent;
  }

  /*
       Notifications
    */
  private void notifyNow(String action){
    if(action == "start"){
      if(notifyStart == true){
        postNotification(notifyStartString[0], notifyStartString[1], true);
      }
    }
    if(action == "stop"){
      if(notifyStop == true){
        postNotification(notifyStopString[0], notifyStopString[1], false);
      }
    }
  }
  private NotificationCompat.Builder getNotificationBuilder(String title, String content) {
    //Onclick
    Intent intent = new Intent(getReactApplicationContext(), this.getCurrentActivity().getClass());
    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
    PendingIntent contentIntent = PendingIntent.getActivity(this.reactContext, 0, intent, 0);
    //Intent intent = new Intent(this.getReactApplicationContext(), NotificationEventReceiver.class);
    //PendingIntent contentIntent = PendingIntent.getBroadcast(this.getReactApplicationContext(), NOTIFICATION_ID_STOP, intent, PendingIntent.FLAG_UPDATE_CURRENT);

    //Build notification
    NotificationCompat.Builder notification = new NotificationCompat.Builder(this.reactContext, CHANNEL_ID)
            .setContentTitle(title)
            .setStyle(new NotificationCompat.BigTextStyle().bigText(content))
            .setContentText(content)
            .setSmallIcon(getReactApplicationContext().getApplicationInfo().icon)
            .setContentIntent(contentIntent);
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
  public void postNotification(String title, String content, boolean start){
    NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this.reactContext);

    int notifyID = NOTIFICATION_ID_STOP;
    if(start == true){
      notifyID = NOTIFICATION_ID_START;
    }
    notificationManager.notify(NOTIFICATION_TAG, notifyID, getNotificationBuilder(title, content).build());
  }
  /*
  private static int getNextNotifId(Context context) {
    SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
    int id = sharedPreferences.getInt(PREFERENCE_LAST_NOTIF_ID, 0) + 1;
    if (id == Integer.MAX_VALUE) { id = 0; }
    sharedPreferences.edit().putInt(PREFERENCE_LAST_NOTIF_ID, id).apply();
    return id;
  }
  */
  //BroadcastReceiver
  public class LocalBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
      Long currentTime = System.currentTimeMillis();
      int duration = intent.getIntExtra("duration", 3000);
      Long startTime = intent.getLongExtra("startTime", System.currentTimeMillis());
      int remainingTime = toIntExact(duration-(currentTime-startTime));
      Log.i(TAG, "Broadcast received");
      Log.i(TAG, "RemainingTimeReceiver: " + remainingTime);
      Intent serviceIntent = new Intent(context, MonitorUpdateService.class);
      serviceIntent.putExtra("remainingTime", remainingTime);
      serviceIntent.putExtra("duration", duration);
      serviceIntent.putExtra("startTime", startTime);
      context.startService(serviceIntent);
      HeadlessJsTaskService.acquireWakeLockNow(context);

      //reactContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
      //        .emit("outOfMonitorGeofence", remainingTime);

    }
  }

}
