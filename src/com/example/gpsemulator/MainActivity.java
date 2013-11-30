//firstChange
package com.example.gpsemulator;

import android.os.Bundle;
import android.app.Activity;
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
	
	BackgroundLocationService currentLocation ;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		currentLocation = new BackgroundLocationService() ;
		
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
					Toast.makeText(getApplicationContext(), "Your Location is - \nLat: " + lat + "\nLong: " + longi, Toast.LENGTH_LONG).show();
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
                gps = new GPSTracker(MainActivity.this);
 
                // check if GPS enabled     
                if(gps.canGetLocation()){
                     
                    double latitude = gps.getLatitude();
                    double longitude = gps.getLongitude();
                     
                    // \n is for new line
                    Toast.makeText(getApplicationContext(), "Your Location is - \nLat: " + latitude + "\nLong: " + longitude, Toast.LENGTH_LONG).show(); 
                    
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

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

}
