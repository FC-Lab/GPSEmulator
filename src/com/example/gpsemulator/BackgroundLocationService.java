package com.example.gpsemulator;


import java.text.DateFormat ;
import java.text.SimpleDateFormat ;
import java.io.* ;
import java.util.Date ;
import java.util.List ;
import java.util.ArrayList ;
import java.util.Arrays ;

import android.app.* ;
import android.os.* ;
import android.content.Intent ;
import android.content.IntentSender ;
import android.content.IntentFilter ;
import android.location.Location ;
import android.util.Log ;
import android.widget.Toast;
import android.support.v4.content.LocalBroadcastManager ;

import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.GooglePlayServicesUtil ;
import com.google.android.gms.common.ConnectionResult ;
import com.google.android.gms.location.* ;
import com.google.android.gms.maps.model.LatLng ;

import com.example.gpsemulator.GeofenceUtils.REQUEST_TYPE ;

import com.firebase.client.* ;

/**
 * BackgroundLocationService used for tracking user location in the background.
 *
 * @author cblack
 */
public class BackgroundLocationService extends Service implements
  GooglePlayServicesClient.ConnectionCallbacks,
  GooglePlayServicesClient.OnConnectionFailedListener,
  LocationClient.OnAddGeofencesResultListener,
	LocationListener {
	
	IBinder mBinder = new LocalBinder();
    Firebase falconhRef ;
	int updateValue ;
    
    private LocationClient mLocationClient;
    private LocationRequest mLocationRequest;
    // Flag that indicates if a request is underway.
    private boolean mInProgress;
    
    private Boolean servicesAvailable = false;
    
    //Geofence related
    List<Geofence> mCurrentGeofences ;
    private SimpleGeofence mGeofence ;
    private GeofenceReceiver mGeofenceBroadcastReceiver ;
    private IntentFilter mIntentFilter ;
    private REQUEST_TYPE mRequestType;
    private PendingIntent mGeofencePendingIntent ;
    List<LatLng> busStop = new ArrayList<LatLng>();
    
    
    public class LocalBinder extends Binder {
    	public BackgroundLocationService getServerInstance() {
    		return BackgroundLocationService.this;
    	}
    }
    
    @Override
	public void onCreate() {
        super.onCreate();
        Log.e("onLocationChanged", "zz");
        falconhRef = new Firebase("https://falconh.firebaseio.com") ;
        updateValue = 0 ;
        falconhRef.child("Update").setValue("0");
        mInProgress = false;
        // Create the LocationRequest object
        mLocationRequest = LocationRequest.create();
        // Use high accuracy
        mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        // Set the update interval to 5 seconds
        mLocationRequest.setInterval(Constants.UPDATE_INTERVAL);
        // Set the fastest update interval to 1 second
        mLocationRequest.setFastestInterval(Constants.FASTEST_INTERVAL);

        servicesAvailable = servicesConnected();
        
        /*
         * Create a new location client, using the enclosing class to
         * handle callbacks.
         */
        mLocationClient = new LocationClient(this, this, this);
        mGeofencePendingIntent = null ;
        
        
        //Geofence related
        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(GeofenceUtils.ACTION_GEOFENCES_ADDED);
        mIntentFilter.addAction(GeofenceUtils.ACTION_GEOFENCES_REMOVED);
        mIntentFilter.addAction(GeofenceUtils.ACTION_GEOFENCE_ERROR);
        mIntentFilter.addCategory(GeofenceUtils.CATEGORY_LOCATION_SERVICES);
        mCurrentGeofences = new ArrayList<Geofence>() ;
        mGeofenceBroadcastReceiver = new GeofenceReceiver();
        
        if(busStop.size() != 0 )
        {
        	busStop.clear();
        }
        
        busStop.add(new LatLng(5.356224,100.300443));
        busStop.add(new LatLng(5.357217,100.300958));
        busStop.add(new LatLng(5.358788,100.3029));
        busStop.add(new LatLng(5.358147,100.304015));
        busStop.add(new LatLng(5.35647,100.303694));
        busStop.add(new LatLng(5.35491,100.302685));
        busStop.add(new LatLng(5.354867,100.300303));
        
        createGeofence();
        
        
        LocalBroadcastManager.getInstance(this).registerReceiver(mGeofenceBroadcastReceiver, mIntentFilter);
        Log.e("BackgroundLocation","Broadcastreceiver registed");
    }
    
    private boolean servicesConnected() {
    	
        // Check that Google Play services is available
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        
        // If Google Play services is available
        if (ConnectionResult.SUCCESS == resultCode) {
            //Toast.makeText(this, "AConnected", Toast.LENGTH_SHORT).show();
        	mRequestType = GeofenceUtils.REQUEST_TYPE.ADD; 
            return true;
        } else {
            //Toast.makeText(this, "ADisConnected", Toast.LENGTH_SHORT).show();
            return false;
        }
    }
    
    public int onStartCommand (Intent intent, int flags, int startId)
    {
        super.onStartCommand(intent, flags, startId);
        Log.e("onLocationChanged", "zz");
        if(!servicesAvailable || mLocationClient.isConnected() || mInProgress)
        	return START_STICKY;
        
        setUpLocationClientIfNeeded();
        if(!mLocationClient.isConnected() || !mLocationClient.isConnecting() && !mInProgress)
        {
        	appendLog(DateFormat.getDateTimeInstance().format(new Date()) + ": Started", Constants.LOG_FILE);
        	mInProgress = true;
        	mLocationClient.connect();
        }
        
        return START_STICKY;
    }
 
	/*
     * Create a new location client, using the enclosing class to
     * handle callbacks.
     */
    private void setUpLocationClientIfNeeded()
    {
    	if(mLocationClient == null) 
            mLocationClient = new LocationClient(this, this, this);
    }
    
    // Define the callback method that receives location updates
    @Override
    public void onLocationChanged(Location location) {
        // Report to the UI that the location was updated
        String msg = Double.toString(location.getLatitude()) + "," +
                Double.toString(location.getLongitude());
        updateValue++;
        falconhRef.child("Latitude").setValue(String.valueOf(location.getLatitude()));
        falconhRef.child("Longitude").setValue(String.valueOf(location.getLongitude()));
        falconhRef.child("Speed").setValue(String.valueOf(location.getSpeed()));
        Log.d("debug", msg);
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
        appendLog(msg, Constants.LOCATION_FILE);
    }
 
    @Override
    public IBinder onBind(Intent intent) {
    	return mBinder;
    }
 
    public String getTime() {
    	SimpleDateFormat mDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    	return mDateFormat.format(new Date());
    }
    
    public void appendLog(String text, String filename)
    {       
       File logFile = new File(filename);
       if (!logFile.exists())
       {
          try
          {
             logFile.createNewFile();
          } 
          catch (IOException e)
          {
             // TODO Auto-generated catch block
             e.printStackTrace();
          }
       }
       try
       {
          //BufferedWriter for performance, true to set append to file flag
          BufferedWriter buf = new BufferedWriter(new FileWriter(logFile, true)); 
          buf.append(text);
          buf.newLine();
          buf.close();
       }
       catch (IOException e)
       {
          // TODO Auto-generated catch block
          e.printStackTrace();
       }
    }
    
    public void createGeofence()
    {
    	if ( mCurrentGeofences.size() != 0)
    		mCurrentGeofences.clear();
    	
    	for( int i = 0 ; i < busStop.size() ; i++ )
    	{
    		mGeofence = new SimpleGeofence(
    					Integer.toString(i),
    					busStop.get(i).latitude,
    					busStop.get(i).longitude,
    					Constants.GEOFENCE_RADIUS,
    					Constants.GEOFENCE_EXPIRATION_IN_MILLISECONDS,
    					Geofence.GEOFENCE_TRANSITION_ENTER |
    	                Geofence.GEOFENCE_TRANSITION_EXIT
    					);
    		
    		mCurrentGeofences.add(mGeofence.toGeofence());
    		}
    }
    
    private PendingIntent createRequestPendingIntent() {

        // If the PendingIntent already exists
        if (null != mGeofencePendingIntent) {

            // Return the existing intent
            return mGeofencePendingIntent;

        // If no PendingIntent exists
        } else {

            // Create an Intent pointing to the IntentService
            Intent intent = new Intent("com.example.gpsemulator.ACTION_RECEIVE_GEOFENCE");
            /*
             * Return a PendingIntent to start the IntentService.
             * Always create a PendingIntent sent to Location Services
             * with FLAG_UPDATE_CURRENT, so that sending the PendingIntent
             * again updates the original. Otherwise, Location Services
             * can't match the PendingIntent to requests made with it.
             */
            return PendingIntent.getBroadcast(
                    getApplicationContext(),
                    0,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT);
        }
    }
    
    @Override
    public void onAddGeofencesResult(int statusCode, String[] geofenceRequestIds) {
    
    Intent broadcastIntent = new Intent();

    // Temp storage for messages
    String msg;

    // If adding the geocodes was successful
    if (LocationStatusCodes.SUCCESS == statusCode) {

        // Create a message containing all the geofence IDs added.
        msg = this.getString(R.string.add_geofences_result_success,
                Arrays.toString(geofenceRequestIds));

        // In debug mode, log the result
        Log.d(GeofenceUtils.APPTAG, msg);

        // Create an Intent to broadcast to the app
        broadcastIntent.setAction(GeofenceUtils.ACTION_GEOFENCES_ADDED)
                       .addCategory(GeofenceUtils.CATEGORY_LOCATION_SERVICES)
                       .putExtra(GeofenceUtils.EXTRA_GEOFENCE_STATUS, msg);
    // If adding the geofences failed
    } else {

        /*
         * Create a message containing the error code and the list
         * of geofence IDs you tried to add
         */
        msg = this.getString(
                R.string.add_geofences_result_failure,
                statusCode,
                Arrays.toString(geofenceRequestIds)
        );

        // Log an error
        Log.e(GeofenceUtils.APPTAG, msg);

        // Create an Intent to broadcast to the app
        broadcastIntent.setAction(GeofenceUtils.ACTION_GEOFENCE_ERROR)
                       .addCategory(GeofenceUtils.CATEGORY_LOCATION_SERVICES)
                       .putExtra(GeofenceUtils.EXTRA_GEOFENCE_STATUS, msg);
    }
    
    LocalBroadcastManager.getInstance(this).sendBroadcast(broadcastIntent);
    
    Log.e("BackgroundLocation","GeofenceResultLastBroadcast");
    
}
    
    @Override
    public void onDestroy(){
        // Turn off the request flag
        mInProgress = false;
        if(servicesAvailable && mLocationClient != null) {
	        mLocationClient.removeLocationUpdates(this);
	        // Destroy the current location client
	        mLocationClient = null;
        }
        // Display the connection status
        // Toast.makeText(this, DateFormat.getDateTimeInstance().format(new Date()) + ": Disconnected. Please re-connect.", Toast.LENGTH_SHORT).show();
        appendLog(DateFormat.getDateTimeInstance().format(new Date()) + ": Stopped", Constants.LOG_FILE);
        super.onDestroy();  
    }
    
    /*
     * Called by Location Services when the request to connect the
     * client finishes successfully. At this point, you can
     * request the current location or start periodic updates
     */
    @Override
    public void onConnected(Bundle bundle) {
    	// Display the connection status
        Log.e("onLocationChanged", "Connected");
        //Toast.makeText(this, "Connected", Toast.LENGTH_SHORT).show();
        // Request location updates using static settings
        mLocationClient.requestLocationUpdates(mLocationRequest, this);
        appendLog(DateFormat.getDateTimeInstance().format(new Date()) + ": Connected", Constants.LOG_FILE);
        
        switch (mRequestType) {
        case ADD :
            // Get the PendingIntent for the request
            mGeofencePendingIntent =
                    createRequestPendingIntent();
            // Send a request to add the current geofences
            mLocationClient.addGeofences(
                    mCurrentGeofences, mGeofencePendingIntent, this);
            
            
            default :
    
        }
    }
 
    /*
     * Called by Location Services if the connection to the
     * location client drops because of an error.
     */
    @Override
    public void onDisconnected() {
        // Turn off the request flag
        mInProgress = false;
        // Destroy the current location client
        mLocationClient = null;
        // Display the connection status
        Toast.makeText(this, DateFormat.getDateTimeInstance().format(new Date()) + ": Disconnected. Please re-connect.", Toast.LENGTH_SHORT).show();
        appendLog(DateFormat.getDateTimeInstance().format(new Date()) + ": Disconnected", Constants.LOG_FILE);
    }
 
    /*
     * Called by Location Services if the attempt to
     * Location Services fails.
     */
    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.e("onLocationChanged", "YE");
    	mInProgress = false;
    	
        /*
         * Google Play services can resolve some errors it detects.
         * If the error has a resolution, try sending an Intent to
         * start a Google Play services activity that can resolve
         * error.
         */
        if (connectionResult.hasResolution()) {
 
        // If no resolution is available, display an error dialog
        } else {
 
        }
    }
    
    
}