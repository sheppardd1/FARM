package com.example.davea.FARM;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Map;

import androidx.appcompat.app.AppCompatActivity;

public class GetInterval extends AppCompatActivity {

    Toast myToast = null;
    boolean GPSRadioIsSet = false;
    boolean MapRadioIsSet = false;
    boolean drawField = false;
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
        CheckBox drawFieldCheckBox = findViewById(R.id.drawFieldCheckBox);
        locationChoiceRadioGroup = findViewById(R.id.LocationChoiceRadioGroup);
        mapChoiceRadioGroup = findViewById(R.id.MapChoiceRadioGroup);


        done.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //note: using an unsigned EditText for interval, so don't have to worry about negative numbers there

                // If user left some options blank, set them to defaults

                if(interval_input.getText().toString().trim().length() != 0){  //if interval is set, store it
                    MapsActivity.interval = Integer.parseInt(interval_input.getText().toString()); //set interval to value specified in interval_input
                    MapsActivity.interval *= 1000;  //convert seconds into milliseconds
                }
                else if (MapsActivity.interval == -1){  //if never set, give it a default value
                    MapsActivity.interval = 1000;
                } //else keep previously set value


                if(width_input.getText().toString().trim().length() != 0){ //if width is set, store it
                    MapsActivity.pathWidth = Float.parseFloat(width_input.getText().toString());
                }
                else if (MapsActivity.pathWidth == -1){   //if not set, give it a default value
                    MapsActivity.pathWidth = 10;
                } // else keep previously set value

                if (!GPSRadioIsSet && MapsActivity.useFusedLocation == null){   // if GPS radio was never set, default to using Fused Location
                    MapsActivity.useFusedLocation = true;
                }// else keep previously set value

                if(!MapRadioIsSet && MapsActivity.useSatellite == null) {    //else Map type radio was never set
                    MapsActivity.useSatellite = true;
                }// else keep previously set value

                // All values are now set

                MapsActivity.setInterval = true;    //ensures that this activity only runs once

                // watch out for previously set location listeners
                if (MapsActivity.locationListener != null) {    //if there is a location listener set up, remove it
                    MapsActivity.locationManager.removeUpdates(MapsActivity.locationListener);  //ensures we only have one location listener running at once. Don't want duplicate data.
                } else if (MapsActivity.useFusedLocation != null && MapsActivity.useFusedLocation.booleanValue() && MapsActivity.myLocationCallback != null){ //if using fused location and myLocaationCallback is not null
                    MapsActivity.myFusedLocationClient.removeLocationUpdates(MapsActivity.myLocationCallback);
                }

                // All good! Go to next activity
                if(drawField){
                    startActivity(new Intent(getApplicationContext(), SelectField.class));
                }
                else {
                    startActivity(new Intent(getApplicationContext(), MapsActivity.class));
                }
            }
        });

        drawFieldCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                drawField = isChecked;
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
