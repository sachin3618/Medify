package com.geekymusketeers.medify;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import com.geekymusketeers.medify.mainFragments.MapsFragment;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;

import java.util.List;

public class GeofenceBroadcastReceiver extends BroadcastReceiver {
    private static final String TAG = "GeofenceBroadcastReceiv";

    @Override
    public void onReceive(Context context, Intent intent) {
        // an Intent broadcast.
       // Toast.makeText(context, "Geofence triggered...", Toast.LENGTH_SHORT).show();

        NotificationHelper notificationHelper = new NotificationHelper(context);

        GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);

        try{
            if (geofencingEvent.hasError()) {
                Log.d(TAG, "onReceive: Error receiving geofence event...");
                return;
            }

            List<Geofence> geofenceList = geofencingEvent.getTriggeringGeofences();
            for (Geofence geofence: geofenceList) {
                Log.d(TAG, "onReceive: " + geofence.getRequestId());
            }
//        Location location = geofencingEvent.getTriggeringLocation();
            int transitionType = geofencingEvent.getGeofenceTransition();

            Log.i("transistion", String.valueOf(transitionType));
            switch (transitionType) {
                case Geofence.GEOFENCE_TRANSITION_ENTER:
                    Toast.makeText(context, "GEOFENCE_TRANSITION_ENTER", Toast.LENGTH_SHORT).show();
                    notificationHelper.sendHighPriorityNotification("GEOFENCE_TRANSITION_ENTER", "", MapActivity.class);
                    break;
                case Geofence.GEOFENCE_TRANSITION_DWELL:
                    Toast.makeText(context, "GEOFENCE_TRANSITION_DWELL", Toast.LENGTH_SHORT).show();
                    notificationHelper.sendHighPriorityNotification("GEOFENCE_TRANSITION_DWELL", "", MapActivity.class);
                    break;
                case Geofence.GEOFENCE_TRANSITION_EXIT:
                    Toast.makeText(context, "GEOFENCE_TRANSITION_EXIT", Toast.LENGTH_SHORT).show();
                    notificationHelper.sendHighPriorityNotification("GEOFENCE_TRANSITION_EXIT", "", MapActivity.class);
                    break;
            }
        }catch (Exception e){
            Log.i("ThrowException", e.getMessage());
        }


    }
}