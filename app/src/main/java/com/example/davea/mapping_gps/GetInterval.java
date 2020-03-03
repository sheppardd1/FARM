package com.example.davea.mapping_gps;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Map;

public class GetInterval extends AppCompatActivity {

    Toast myToast = null;
    boolean radioIsSet = false;
    RadioGroup locationChoiceRadioGroup;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_get_interval);


        //setup UI
        final EditText interval_input = findViewById(R.id.interval_input);
        final EditText lat_input = findViewById(R.id.lat_input);
        final EditText lng_input = findViewById(R.id.lng_input);
        Button done = findViewById(R.id.done);
        TextView TV = findViewById(R.id.instructions);
        locationChoiceRadioGroup = findViewById(R.id.LocationChoiceRadioGroup);

        //print instructions
        TV.setText(R.string.GetInterval_Instructions);

        done.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean validValues; //only false if lat and lng values are both inputted and at least one is outside of acceptable range, else true
                //note: using an unsigned EditText for interval, so don't have to worry about negative numbers there
                if(interval_input.getText().toString().trim().length() > 0 && radioIsSet) {   //ensure interval_input is not empty and radio button is set
                    MapsActivity.interval = Integer.valueOf(interval_input.getText().toString()); //set interval to value specified in interval_input
                    MapsActivity.interval *= 1000;  //convert seconds into milliseconds
                    MapsActivity.setInterval = true;    //ensures that this activity only runs once
                    if (MapsActivity.locationListener != null) {    //if there is a location listener set up, remove it
                        MapsActivity.locationManager.removeUpdates(MapsActivity.locationListener);  //ensures we only have one location listener running at once. Don't want duplicate data.
                    }
                    //if user has entered both a true lat and long value, set them
                    //if entered only one or none, ignore it
                    if(lat_input.getText().toString().trim().length() > 0 && lng_input.getText().toString().trim().length() > 0){
                        if(Math.abs(Float.valueOf(lat_input.getText().toString())) > 90)
                        {   //lat values must be between -90 and 90 inclusive
                            if(myToast != null) myToast.cancel();
                            myToast = Toast.makeText(getApplicationContext(), "Invalid Latitude Value", Toast.LENGTH_SHORT);
                            myToast.show();
                            validValues = false;
                        }
                        else if(Math.abs(Float.valueOf(lng_input.getText().toString())) > 180)
                        {   //lng values must be between -180 and 180 inclusive
                            if(myToast != null) myToast.cancel();
                            myToast = Toast.makeText(getApplicationContext(), "Invalid Longitude Value", Toast.LENGTH_SHORT);
                            myToast.show();
                            validValues = false;
                        }
                        else {
                            validValues = true;
                            MapsActivity.setTrueLatLng = true;
                            //set values in MapsActivity
                            MapsActivity.trueLat = Double.valueOf(lat_input.getText().toString());
                            MapsActivity.trueLng = Double.valueOf(lng_input.getText().toString());
                        }
                    }
                    else {  //else did not specify both true lat and long
                        MapsActivity.setTrueLatLng = false;
                        validValues = true;
                    }
                    if(validValues) {
                        //go back to main activity
                        startActivity(new Intent(getApplicationContext(), MapsActivity.class));
                    }
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

        // Check which radio button was clicked
        switch(view.getId()) {
            case R.id.radio_GPS:
                if (checked) {
                    MapsActivity.usingCriteria = false;
                    MapsActivity.useFusedLocation = false;
                    radioIsSet = true;  //once a radio button is checked, user cannot uncheck all buttons
                }
                    break;
            case R.id.radio_CriteriaGPS:
                if (checked){
                    MapsActivity.usingCriteria = true;
                    MapsActivity.useFusedLocation = false;
                    radioIsSet = true;
                }
                    break;
            case R.id.radio_Fused:
            if (checked) {
                MapsActivity.usingCriteria = false;
                MapsActivity.useFusedLocation = true;
                    radioIsSet = true;  //once a radio button is checked, user cannot uncheck all buttons
                }
            break;
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        locationChoiceRadioGroup.clearCheck();  //clear radio group choices when leaving activity
    }

    @Override
    protected void onPause(){
        super.onPause();
        locationChoiceRadioGroup.clearCheck();  //clear radio group choices when leaving activity

    }

}
