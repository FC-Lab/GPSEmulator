//firstChange
package com.example.gpsemulator;

import android.os.Bundle;
import android.app.Activity;
import android.app.ActivityManager ;
import android.app.ActivityManager.* ;
import android.content.Context ;
import android.content.Intent;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;
import com.firebase.client.* ;
import java.util.Map ;

public class MainActivity extends Activity {
	Button btnShowLocation ;
	
	GPSTracker gps ;
	
	Firebase falconhRef ;
	
	Intent mServiceIntent ;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		mServiceIntent = new Intent(this,BackgroundLocationService.class);
		this.startService(mServiceIntent);
		
		
		
		falconhRef = new Firebase("https://falconh.firebaseio.com") ;
		
		falconhRef.addValueEventListener(new ValueEventListener() {
			@Override
			public void onDataChange( DataSnapshot snapshot )
			{
				Object value = snapshot.getValue();
				if ( value != null )
				{
					String lat = (String)((Map)value).get("Latitude");
					String longi = (String)((Map)value).get("Longitude");
					//Toast.makeText(getApplicationContext(), "Your Location is - \nLat: " + lat + "\nLong: " + longi, Toast.LENGTH_SHORT).show();
				}
			}

			@Override
			public void onCancelled(FirebaseError arg0) {
				// TODO Auto-generated method stub
				
			}
		});
		
		btnShowLocation = (Button) findViewById(R.id.btnShowLocation);
        
        // show location button click event
        btnShowLocation.setOnClickListener(new View.OnClickListener() {
             
            @Override
            public void onClick(View arg0) {        
                // create class object
            	
            	if ( isMyServiceRunning() )
            	{
            		 Toast.makeText(getApplicationContext(), "Service is running", Toast.LENGTH_SHORT).show();
            	}
            	else
            	{
            		Toast.makeText(getApplicationContext(), "Service is not running", Toast.LENGTH_SHORT).show();
            	}
            	
                gps = new GPSTracker(MainActivity.this);
 
                // check if GPS enabled     
                if(gps.canGetLocation()){
                     
                    double latitude = gps.getLatitude();
                    double longitude = gps.getLongitude();
                     
                    // \n is for new line
                    Toast.makeText(getApplicationContext(), "Location is - \nLat: " + latitude + "\nLong: " + longitude, Toast.LENGTH_SHORT).show(); 
                    
                    falconhRef.child("Latitude").setValue(String.valueOf(latitude)) ;
                    falconhRef.child("Longitude").setValue(String.valueOf(longitude));
                    
                }else{
                    // can't get location
                    // GPS or Network is not enabled
                    // Ask user to enable GPS/network in settings
                    gps.showSettingsAlert();
                }
                 
            }
        });
	}
	
	
	private boolean isMyServiceRunning() {
	    ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
	    for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
	        if (BackgroundLocationService.class.getName().equals(service.service.getClassName())) {
	            return true;
	        }
	    }
	    return false;
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

}
