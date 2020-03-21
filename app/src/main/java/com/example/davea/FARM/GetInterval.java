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
    boolean radioIsSet = false;
    RadioGroup locationChoiceRadioGroup;
    RadioGroup mapChoiceRadioGroup;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_get_interval);


        //setup UI
        final EditText interval_input = findViewById(R.id.interval_input);
        Button done = findViewById(R.id.done);
        TextView TV1 = findViewById(R.id.interval_instructions);
        TextView TV2 = findViewById(R.id.method_instructions);
        TextView TV3 = findViewById(R.id.mapType_instructions);
        locationChoiceRadioGroup = findViewById(R.id.LocationChoiceRadioGroup);
        mapChoiceRadioGroup = findViewById(R.id.MapChoiceRadioGroup);

        //print instructions
        TV1.setText(R.string.GetInterval_Instructions);
        TV2.setText(R.string.Method_Instructions);
        TV3.setText(R.string.MapType_Instructions);


        done.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //note: using an unsigned EditText for interval, so don't have to worry about negative numbers there
                if(interval_input.getText().toString().trim().length() > 0 && radioIsSet) {   //ensure interval_input is not empty and radio button is set
                    MapsActivity.interval = Integer.valueOf(interval_input.getText().toString()); //set interval to value specified in interval_input
                    MapsActivity.interval *= 1000;  //convert seconds into milliseconds
                    MapsActivity.setInterval = true;    //ensures that this activity only runs once
                    if (MapsActivity.locationListener != null) {    //if there is a location listener set up, remove it
                        MapsActivity.locationManager.removeUpdates(MapsActivity.locationListener);  //ensures we only have one location listener running at once. Don't want duplicate data.
                    } else if (MapsActivity.useFusedLocation && MapsActivity.myLocationCallback != null){ //if using fused location and myLocaationCallback is not null
                        MapsActivity.myFusedLocationClient.removeLocationUpdates(MapsActivity.myLocationCallback);
                    }
                    startActivity(new Intent(getApplicationContext(), MapsActivity.class)); // go to mapping activity
                }
                else if(radioIsSet){ //if radio is set, then interval is not
                    if(myToast != null) myToast.cancel();
                    myToast = Toast.makeText(getApplicationContext(), "Must Enter Interval Value", Toast.LENGTH_SHORT);
                    myToast.show();
                }
                else{ //else radio is not set
                    if(myToast != null) myToast.cancel();
                    myToast = Toast.makeText(getApplicationContext(), "Must choose location type\n(GPS or Fused)", Toast.LENGTH_SHORT);
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
                    radioIsSet = true;  //once a radio button is checked, user cannot uncheck all buttons
                }
            break;
            case R.id.radio_Fused:
            if (checked) {
                MapsActivity.useFusedLocation = true;
                    radioIsSet = true;  //once a radio button is checked, user cannot uncheck all buttons
                }
            break;
        }
        // Check which map type user chose
        switch(view.getId()) {
            case R.id.radio_regular:
                if (checked) {
                    MapsActivity.useSatellite = false;
                    radioIsSet = true;  //once a radio button is checked, user cannot uncheck all buttons
                }
                break;
            case R.id.radio_satellite:
                if (checked) {
                    MapsActivity.useSatellite = true;
                    radioIsSet = true;  //once a radio button is checked, user cannot uncheck all buttons
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
