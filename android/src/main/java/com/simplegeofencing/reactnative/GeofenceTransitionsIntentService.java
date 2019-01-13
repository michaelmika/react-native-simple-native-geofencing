package com.simplegeofencing.reactnative;


import android.app.IntentService;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;
import com.facebook.react.bridge.ReactApplicationContext;
import java.util.ArrayList;
import java.util.List;

public class GeofenceTransitionsIntentService extends IntentService {
    private static final String TAG = "GeofenceService";
    private final String CHANNEL_ID = "channel_01";
    private int notificationId = 100;
    private NotificationChannel channel;
    public GeofenceTransitionsIntentService(){
        super(TAG);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "Intent created");
    }


    protected void onHandleIntent(Intent intent) {
        Log.i(TAG, "onHandleIntent");
        GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);
        if (geofencingEvent.hasError()) {
            String errorMessage = "Error Code: " + String.valueOf(geofencingEvent.getErrorCode());
            Log.e(TAG, errorMessage);
            return;
        }
        // Get the transition type.
        int geofenceTransition = geofencingEvent.getGeofenceTransition();

        // Test that the reported transition was of interest.
        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER ||
                geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT) {

            // Get the geofences that were triggered. A single event can trigger
            // multiple geofences.
            List<Geofence> triggeringGeofences = geofencingEvent.getTriggeringGeofences();

            // Get the transition details as a String.
            final String geofenceTransitionDetails = getGeofenceTransitionDetails(
                    geofenceTransition,
                    triggeringGeofences
            );

            if(geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER &&
                    intent.getBooleanExtra("notifyEnter", false)){
                postNotification(
                        intent.getStringExtra("notifyEnterStringTitle"),
                        geofenceTransitionDetails,
                        intent.getStringExtra("notifyChannelStringTitle"),
                        intent.getStringExtra("notifyChannelStringDescription")
                );
            }
            if(geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT &&
                    intent.getBooleanExtra("notifyExit", false)){
                postNotification(
                        intent.getStringExtra("notifyExitStringTitle"),
                        geofenceTransitionDetails,
                        intent.getStringExtra("notifyChannelStringTitle"),
                        intent.getStringExtra("notifyChannelStringDescription")
                );
            }

            Log.i(TAG, geofenceTransitionDetails);
            //RNSimpleNativeGeofencingModule.postNotification(TAG, geofenceTransitionDetails);
        } else {
            // Log the error.
            Log.e(TAG, "Invalid transition");
            postNotification(
                    "Error",
                    "Inside Handel",
                    intent.getStringExtra("notifyChannelStringTitle"),
                    intent.getStringExtra("notifyChannelStringDescription")
            );
        }
    }

    /*
        Helpfunctions for logging
     */
    private String getGeofenceTransitionDetails(
            int geofenceTransition,
            List<Geofence> triggeringGeofences) {

        String geofenceTransitionString = getTransitionString(geofenceTransition);

        // Get the Ids of each geofence that was triggered.
        ArrayList<String> triggeringGeofencesIdsList = new ArrayList<>();
        for (Geofence geofence : triggeringGeofences) {
            triggeringGeofencesIdsList.add(geofence.getRequestId());
        }
        String triggeringGeofencesIdsString = TextUtils.join(", ",  triggeringGeofencesIdsList);

        return geofenceTransitionString + ": " + triggeringGeofencesIdsString;
    }
    private String getTransitionString(int transitionType) {
        switch (transitionType) {
            case Geofence.GEOFENCE_TRANSITION_ENTER:
                return "entered Geofence";
            case Geofence.GEOFENCE_TRANSITION_EXIT:
                return "exit Geofence";
            default:
                return "unknown Transition";
        }
    }

    /*
       Notifications
    */
    private NotificationCompat.Builder getNotificationBuilder(String title,
                                                              String content,
                                                              String channelTitle,
                                                              String channelDescription) {
        //Onclick
        //Intent intent = new Intent(this.reactContext, AlertDetails.class);
        //intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        //PendingIntent pendingIntent = PendingIntent.getActivity(this.reactContext, 0, intent, 0);

        //Build notification
        NotificationCompat.Builder notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(content)
                .setSmallIcon(this.getApplicationInfo().icon)
                .setAutoCancel(true);
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && channel==null) {
            CharSequence name = channelTitle;
            String description = channelDescription;
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = this.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
            notification.setChannelId(CHANNEL_ID);
        }
        return notification;
    }
    public void postNotification(String Title,
                                 String Content,
                                 String channelTitle,
                                 String channelDescription){
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);

        // notificationId is a unique int for each notification that you must define
        notificationManager.notify(notificationId,
                getNotificationBuilder(Title, Content, channelTitle, channelDescription).build());
        notificationId++;
    }
}
