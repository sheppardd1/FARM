package com.example.davea.FARM;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class GetInterval extends AppCompatActivity {

    Toast myToast = null;
    boolean GPSRadioIsSet = false;
    boolean MapRadioIsSet = false;
    RadioGroup locationChoiceRadioGroup;
    RadioGroup mapChoiceRadioGroup;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_get_interval);


        //setup UI
        final EditText interval_input = findViewById(R.id.interval_input);
        final EditText width_input = findViewById(R.id.width_input);
        Button done = findViewById(R.id.done);
        locationChoiceRadioGroup = findViewById(R.id.LocationChoiceRadioGroup);
        mapChoiceRadioGroup = findViewById(R.id.MapChoiceRadioGroup);


        done.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //note: using an unsigned EditText for interval, so don't have to worry about negative numbers there
                //Ensure interval_input and width_input are not empty and radio button is set
                if(interval_input.getText().toString().trim().length() > 0
                        && width_input.getText().toString().trim().length() > 0 && GPSRadioIsSet && MapRadioIsSet) {

                    // set width and interval values
                    MapsActivity.pathWidth = Float.parseFloat(width_input.getText().toString());   //set path width
                    MapsActivity.interval = Integer.parseInt(interval_input.getText().toString()); //set interval to value specified in interval_input
                    MapsActivity.interval *= 1000;  //convert seconds into milliseconds
                    MapsActivity.setInterval = true;    //ensures that this activity only runs once

                    // watch out for previously set location listeners
                    if (MapsActivity.locationListener != null) {    //if there is a location listener set up, remove it
                        MapsActivity.locationManager.removeUpdates(MapsActivity.locationListener);  //ensures we only have one location listener running at once. Don't want duplicate data.
                    } else if (MapsActivity.useFusedLocation && MapsActivity.myLocationCallback != null){ //if using fused location and myLocaationCallback is not null
                        MapsActivity.myFusedLocationClient.removeLocationUpdates(MapsActivity.myLocationCallback);
                    }

                    // All good! Go to mapping activity.
                    startActivity(new Intent(getApplicationContext(), MapsActivity.class));

                }
                else if(interval_input.getText().toString().trim().length() == 0){  //if interval not set
                    if(myToast != null) myToast.cancel();
                    myToast = Toast.makeText(getApplicationContext(), "Must Enter Interval Value", Toast.LENGTH_SHORT);
                    myToast.show();
                }
                else if(width_input.getText().toString().trim().length() == 0){ //if width not set
                    if(myToast != null) myToast.cancel();
                    myToast = Toast.makeText(getApplicationContext(), "Must Enter Width Value", Toast.LENGTH_SHORT);
                    myToast.show();
                }
                else if (!GPSRadioIsSet){   // if GPS radio is not set
                    if(myToast != null) myToast.cancel();
                    myToast = Toast.makeText(getApplicationContext(), "Must choose location type\n(GPS or Fused)", Toast.LENGTH_SHORT);
                    myToast.show();
                }
                else {    //else Map type radio is not set
                    if(myToast != null) myToast.cancel();
                    myToast = Toast.makeText(getApplicationContext(), "Must choose map type\n(regular or satellite)", Toast.LENGTH_SHORT);
                    myToast.show();
                }
            }
        });
    }

    public void onRadioButtonClicked(View view) {
        // Is the button now checked?
        boolean checked = ((RadioButton) view).isChecked();

        // Check which location method radio button was clicked
        switch(view.getId()) {
            case R.id.radio_GPS:
                if (checked) {
                    MapsActivity.useFusedLocation = false;
                    GPSRadioIsSet = true;  //once a radio button is checked, user cannot uncheck all buttons
                }
            break;
            case R.id.radio_Fused:
            if (checked) {
                    MapsActivity.useFusedLocation = true;
                    GPSRadioIsSet = true;  //once a radio button is checked, user cannot uncheck all buttons
                }
            break;
        }
        // Check which map type user chose
        switch(view.getId()) {
            case R.id.radio_regular:
                if (checked) {
                    MapsActivity.useSatellite = false;
                    MapRadioIsSet = true;  //once a radio button is checked, user cannot uncheck all buttons
                }
                break;
            case R.id.radio_satellite:
                if (checked) {
                    MapsActivity.useSatellite = true;
                    MapRadioIsSet = true;  //once a radio button is checked, user cannot uncheck all buttons
                }
                break;
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        locationChoiceRadioGroup.clearCheck();  //clear radio group choices when leaving activity
        mapChoiceRadioGroup.clearCheck();  //clear radio group choices when leaving activity
    }

    @Override
    protected void onPause(){
        super.onPause();
        locationChoiceRadioGroup.clearCheck();  //clear radio group choices when leaving activity
        mapChoiceRadioGroup.clearCheck();  //clear radio group choices when leaving activity
    }

}
