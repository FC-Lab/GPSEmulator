package com.example.gpsemulator;

import android.content.Context ;
import android.content.Intent ;
import android.content.BroadcastReceiver ;
import android.text.TextUtils ;
import android.util.Log ;
import android.widget.Toast ;
import android.support.v4.content.LocalBroadcastManager ;
import android.content.res.Resources ;

import java.util.List ;

import com.google.android.gms.location.LocationClient ;
import com.google.android.gms.location.Geofence ;

import com.firebase.client.* ;

public class GeofenceReceiver extends BroadcastReceiver {
    /*
     * Define the required method for broadcast receivers
     * This method is invoked when a broadcast Intent triggers the receiver
     */
	Context context ;
	
	Intent broadcastIntent = new Intent() ;
	
	Resources res ;
	
	Firebase falconhRef = new Firebase("https://falconh.firebaseio.com") ;
	
    @Override
    public void onReceive(Context context, Intent intent) {
    	
    	this.context = context ;
        // Check the action code and determine what to do
    	broadcastIntent.addCategory(GeofenceUtils.CATEGORY_LOCATION_SERVICES);
    	
    	if (LocationClient.hasError(intent)) {
            handleError(intent);
        } else {
            handleGeofenceTransition(intent);
        }
    }

    /**
     * If you want to display a UI message about adding or removing geofences, put it here.
     *
     * @param context A Context for this component
     * @param intent The received broadcast Intent
     */
    private void handleGeofenceStatus(Intent intent) {

    }

    /**
     * Report geofence transitions to the UI
     *
     * @param context A Context for this component
     * @param intent The Intent containing the transition
     */
    private void handleGeofenceTransition(Intent intent) {
        /*
         * If you want to change the UI when a transition occurs, put the code
         * here. The current design of the app uses a notification to inform the
         * user that a transition has occurred.
         */
    	int transition = LocationClient.getGeofenceTransition(intent);

        // Test that a valid transition was reported
        if ((transition == Geofence.GEOFENCE_TRANSITION_ENTER)
                || (transition == Geofence.GEOFENCE_TRANSITION_EXIT)) {

            // Post a notification
            List<Geofence> geofences = LocationClient
                    .getTriggeringGeofences(intent);
            String[] geofenceIds = new String[geofences.size()];
            
            for( int i = 0 ; i < geofences.size() ; i++ )
            {
            	geofenceIds[i] = geofences.get(i).getRequestId() ;
            	falconhRef.child("Update").setValue(geofenceIds) ;
            }
            
            String ids = TextUtils.join(GeofenceUtils.GEOFENCE_ID_DELIMITER,
                    geofenceIds);
            String transitionType = getTransitionString(transition);
            
            
            
        }
    	
    	
    }

    /**
     * Report addition or removal errors to the UI, using a Toast
     *
     * @param intent A broadcast Intent sent by ReceiveTransitionsIntentService
     */
    private void handleError(Intent intent) {
    	
        String msg = intent.getStringExtra(GeofenceUtils.EXTRA_GEOFENCE_STATUS);
        Log.e(GeofenceUtils.APPTAG, msg);
        Toast.makeText(context, msg, Toast.LENGTH_LONG).show();
    }
    
    private String getTransitionString(int transitionType) {
        switch (transitionType) {

            case Geofence.GEOFENCE_TRANSITION_ENTER:
                return res.getString(R.string.geofence_transition_entered);

            case Geofence.GEOFENCE_TRANSITION_EXIT:
                return res.getString(R.string.geofence_transition_exited);

            default:
                return res.getString(R.string.geofence_transition_unknown);
        }
    }
}
