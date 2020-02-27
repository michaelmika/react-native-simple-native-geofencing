package com.simplegeofencing.reactnative;


import android.app.IntentService;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.IBinder;
import android.preference.PreferenceManager;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import android.util.Log;

public class ShowTimeoutNotification extends IntentService {
    private static final String TAG = "GeofenceTimeout";
    private NotificationChannel channel;
    private static final String PREFERENCE_LAST_NOTIF_ID = "PREFERENCE_LAST_NOTIF_ID";
    private final String CHANNEL_ID = "channel_01";

    public ShowTimeoutNotification(){
        super(TAG);
    }

    @Override
    public void onCreate() {
        super.onCreate();

        Log.i(TAG, "Timeout of Geofences");
    }

    protected void onHandleIntent(Intent intent){
        //Notify for timeout
        postNotification(
                intent.getStringExtra("notifyStringTitle"),
                intent.getStringExtra("notifyStringDescription"),
                intent.getStringExtra("notifyChannelStringTitle"),
                intent.getStringExtra("notifyChannelStringDescription")
        );
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
                .setLargeIcon(BitmapFactory.decodeResource(this.getResources(),
                        this.getApplicationInfo().icon))
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
        notificationManager.notify(getNextNotifId(this.getApplicationContext()),
                getNotificationBuilder(Title, Content, channelTitle, channelDescription).build());
    }


    private static int getNextNotifId(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        int id = sharedPreferences.getInt(PREFERENCE_LAST_NOTIF_ID, 0) + 1;
        if (id == Integer.MAX_VALUE) { id = 0; }
        sharedPreferences.edit().putInt(PREFERENCE_LAST_NOTIF_ID, id).apply();
        return id;
    }
}
