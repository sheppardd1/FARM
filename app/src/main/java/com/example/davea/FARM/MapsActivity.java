package com.example.davea.FARM;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;

import static com.google.maps.android.SphericalUtil.computeHeading;
import static com.google.maps.android.SphericalUtil.computeOffset;
import static com.google.maps.android.SphericalUtil.computeOffsetOrigin;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, GoogleApiClient.OnConnectionFailedListener {

    //Google Map:
    public GoogleMap gMap;

    //Constants:
    static final String filename = "GPS_data.txt";  //name of file where data is saved
    final int POLYGON_PADDING_PREFERENCE = 10;      // padding around user-defined field that map zooms to
    final double METERS_PER_FEET = 0.3048;           // for converting from ft to m

    //Location:
    //For GPS only:
    public Location currentLocation;
    static LocationManager locationManager;
    static LocationListener locationListener;
    //For Fused Location Provider:
    public static FusedLocationProviderClient myFusedLocationClient;
    LocationRequest myLocationRequest;
    public static LocationCallback myLocationCallback;
    LocationResult currentLocationResult;

    //Files:
    File dataFile;

    //UI:
    Button startBtn, resetBtn, settingsBtn;  //self-explanatory buttons
    TextView TV;    //only textview

    //variables:
    //double and Double:
    double currentLongitude;    //current lng
    double currentLatitude;     //current lat

    //int:
    static int interval = -1;    //refresh rate of GPS
    //float:
    public static float pathWidth = -1; // width of tractor in feet
    public static float offset = 0;     // offset of location data
    // use positive value to offset the app's path to the right and negative to offset to the left

    //String:
    static String fileContents; //Stuff that will be written to the file. It is static so that it can be accessed in other activity
    String time;    //the time in the dateFormatDayAndTime format (defined later). Used for giving start and end times of each session
    LinkedList<String> timeList = new LinkedList<>(); //List of all the times that the datapoints were taken
    //boolean:
    boolean wasReset = false;    //true if session data has been reset
    boolean setStartTime = false;   //true if start time of session has been set. Ensures that start time is only set at the beginning of a session
    public boolean on = false;  //true if session is running, not paused or stopped
    boolean zoomed = false; //true if camera has zoomed in on location yet
    boolean locationPermissionGranted = false;  //true once location permission is granted. Used to ensure that location updates are only requested once
    boolean paused = false; //only true if paused (not running or stopped). Used to prevent ViewData activity from starting while paused
    static Boolean useFusedLocation = null; //true if user selects radio button for Fused Location Services, false if selected GPS
    static boolean setInterval = false; //true if user has specified GPS refresh rate
    public static Boolean useSatellite = null; // true if user opts to view map in satellite mode
    //LatLng:
    LinkedList<LatLng> positionList = new LinkedList<>();
    //Toast:
    Toast myToast = null;

    //Time formatting:
    //create calendar to convert epoch time to readable time
    Calendar cal;
    //create simple date format to show just 12hr time. Defined later on.
    SimpleDateFormat dateFormatTime;
    SimpleDateFormat dateFormatDayAndTime;

    //criteria for location:
    Criteria locationCriteria = new Criteria();

    // polygon formatting
    private static final int COLOR_BLACK_ARGB = 0x7f000000;
    private static final int COLOR_WHITE_ARGB = 0x3fffffff;
    private static final int COLOR_GREEN_ARGB = 0x7f388E3C;
    private static final int COLOR_PURPLE_ARGB = 0x7f81C784;
    private static final int COLOR_ORANGE_ARGB = 0xC0F57F17;
    private static final int COLOR_BLUE_ARGB = 0x7fF9A825;

    private static final int POLYGON_STROKE_WIDTH_PX = 2;
    private static final int PATTERN_DASH_LENGTH_PX = 20;
    private static final int PATTERN_GAP_LENGTH_PX = 20;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        assert mapFragment != null;
        mapFragment.getMapAsync(this);

        setup();    //initialize everything
        assert interval >= 0;
        assert pathWidth >= 0;

        if (interval == 0 && setInterval) {
            //if user sets update interval to 0, warn of battery drain
            if (myToast != null)
                myToast.cancel();   //if other toasts are up, get rid of them to avoid accumulation
            myToast = Toast.makeText(MapsActivity.this, "WARNING: UPDATE INTERVAL IS SET TO 0." +
                    "\n(CONTINUOUS UPDATES)\nTHIS MAY RESULT IN HIGH POWER CONSUMPTION", Toast.LENGTH_SHORT);
            myToast.show();
        }

        startBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                on = !on;   //invert on. Toggles between start and paused
                if (on) {
                    TV.setText(R.string.running);  //display session status
                    locationDetails();  //get location info
                    if (wasReset) { //set wasReset to false
                        wasReset = false;
                    }
                } else {
                    TV.setText(R.string.Paused);    //if not on after pressing start, session must be paused
                }
            }
        });

        resetBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (on) {
                    on = false; //if ON, pause
                    paused = true;
                    TV.setText(R.string.Paused3);
                } else {    // if already paused, reset everything and clear map
                    TV.setText(R.string.PressStart);
                    reset();
                    on = false;
                    gMap.clear();
                    drawField();
                    paused = false;
                }
            }
        });

        settingsBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!on && !paused)
                    startActivity(new Intent(getApplicationContext(), GetInterval.class));    //go to map boundary setting activity
                else {
                    if (myToast != null) myToast.cancel();
                    myToast = Toast.makeText(MapsActivity.this, "Must stop process before going to settings", Toast.LENGTH_SHORT);
                    myToast.show();
                }
            }
        });
    }

    /********************************************************************
     *Activity Functions:
     ********************************************************************/

    @Override
    protected void onResume() {
        super.onResume();
        reset();
        if (gMap != null) gMap.clear();
        TV.setText(R.string.PressStart);
    }

    @Override
    protected void onPause() {  //remove location updates when paused
        super.onPause();
        //remove updates
        if (useFusedLocation != null && useFusedLocation.booleanValue() && myLocationCallback != null) { //if using fused location and myLocaationCallback is not null
            myFusedLocationClient.removeLocationUpdates(myLocationCallback);
        } else if (useFusedLocation != null && !useFusedLocation.booleanValue() && locationListener != null) { //if using GPS only and locationListener is not null
            locationManager.removeUpdates(locationListener); //stop location updates, also ensures no duplicate update requests
        }
        //else methods needed for location have not been instantiated (are still null) so we don't need to remove location updates (if we try, app will crash)
        TV.setText("Program Paused");

    }

    @Override
    protected void onStop() {
        super.onStop();
        //remove updates
        if (useFusedLocation != null && useFusedLocation.booleanValue() && myLocationCallback != null) { //if using fused location and myLocaationCallback is not null
            myFusedLocationClient.removeLocationUpdates(myLocationCallback);
        } else if (useFusedLocation != null && !useFusedLocation.booleanValue() && locationListener != null) { //if using GPS only and locationListener is not null
            locationManager.removeUpdates(locationListener); //stop location updates, also ensures no duplicate update requests
        }
        //else methods needed for location have not been instantiated (are still null) so we don't need to remove location updates (if we try, app will crash)
        TV.setText("Program Stopped");
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        if (myToast != null)
            myToast.cancel();   //if other toasts are up, get rid of them to avoid accumulation
        Toast.makeText(this, "GPS Connection Failure", Toast.LENGTH_SHORT);
        myToast.show();
    }


    /********************************************************************
     *Functions used regardless of location method:
     ********************************************************************/


    public void setup() {   //initializes basic vars and UI stuff

        if (!setInterval) {
            startActivity(new Intent(getApplicationContext(), GetInterval.class));
        }

        cal = Calendar.getInstance();   //instantiate a calendar
        //define the data formats
        //not the best method since it may not work in all places outside U.S., but it's good enough for now
        dateFormatTime = new SimpleDateFormat("HH:mm:ss");
        dateFormatDayAndTime = new SimpleDateFormat("MMM dd, yyyy hh:mm aa");

        //empty the lists
        timeList.clear();

        if (useFusedLocation != null && !useFusedLocation) {
            locationManager = (LocationManager) getSystemService(LOCATION_SERVICE); //set up location manager
        } else if (useFusedLocation != null) {
            myFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        }

        //define buttons and textview
        startBtn = findViewById(R.id.btnStartStop);
        resetBtn = findViewById(R.id.btnReset);
        settingsBtn = findViewById(R.id.btnViewData);
        TV = findViewById(R.id.TV);

        TV.setText(R.string.PressStart); //print starting message in textview

        dataFile = new File(filename);//create file

        if (useFusedLocation != null && !useFusedLocation) {  // if using GPS, set high accuracy criteria
            setCriteria();
        }

    }

    public void reset() {   //resets all values and calls write function

            /* TODO: may eventually write stats to a file for later access
            FileOutputStream outputStream;  //declare output stream
            try {   //attempt to open and write to file
                outputStream = openFileOutput(filename, Context.MODE_PRIVATE);  //open file and set to output stream

                for (int i = 0; i < numPins; i++) {
                    writeData(i, outputStream); //write data
                }
                //empty the lists

                outputStream.close(); //close file

            } catch (Exception e) { //if file is not found, catch exception
                e.printStackTrace();
                TV.setText("ERROR: \nDATA NOT WRITTEN");  //error message if file not found
            }
            */

        TV.setText(R.string.PressStart);    // tell user to press start when ready

        //reset values
        timeList.clear();
        positionList.clear();
        wasReset = true;
        setStartTime = false;
        zoomed = false;
        locationPermissionGranted = false; // ensures that location updates are restarted by forcing locationDetails() to call a getPermissions function

        //remove updates (saves battery)
        if (useFusedLocation != null && useFusedLocation && myLocationCallback != null) { //if using fused location and myLocaationCallback is not null
            myFusedLocationClient.removeLocationUpdates(myLocationCallback);
        } else if (useFusedLocation != null && !useFusedLocation && locationListener != null) { //if using GPS only and locationListener is not null
            locationManager.removeUpdates(locationListener); //stop location updates, also ensures no duplicate update requests
        } //else methods needed for location have not been instantiated (are still null) so we don't need to remove location updates (if we try, app will crash))
    }

    public void writeData(int i, FileOutputStream outputStream) {   //writes data to a .txt file
        /*
        //print header:
        if (i == 0) {    //print time range of data points as header of data
            if (fileContents != null)   //if file already has data in there, use "+=" to add to it
                fileContents += "------------------------------\n Start: " + time + "\n";
                //if file was empty to begin with, we don't want to print out "null" at the beginning, so use "=" instead of "+="
            else fileContents = "------------------------------\n Start: " + time + "\n";

            //convert epoch time to calendar data
            if(useFusedLocation) cal.setTimeInMillis(currentLocationResult.getLastLocation().getTime());
            else cal.setTimeInMillis(currentLocation.getTime());

            //print accuracy value on screen along with coordinates and time
            time = dateFormatDayAndTime.format(cal.getTime());
            fileContents += " Stop:  " + time + "\n------------------------------\n";
            //tell what type of location services are being used
            if(useFusedLocation){
                fileContents += "Using Fused Location Services\n";
            }
            else if (usingCriteria){
                fileContents += "Using GPS with High Accuracy Criteria\n";
            }
            else{
                fileContents += "Using GPS without Criteria\n";
            }

            fileContents += "#  | Accuracy | Time\n";
        }

        //set fileContents to number, accuracy value, and timestamp [example: "#1)  9.0"  ] with fancy formatting
        //fileContents += ("#" + (i + 1) + ") \t\t" +(accuracyList.get(i).toString() + " \t\t" + (String.format("%.2f", distanceErrorList.get(i))) + " \t\t" + timeList.get(i) + "\n"));

        fileContents += String.format("%-7s %s", ("#" + (i + 1) + ")"), String.format("%-10s %s", (timeList.get(i) + "\n")));


        if (i == numPins - 1) {  //end of data that must be written is reached
            fileContents += "\nAverage Accuracy Radius: " + String.format("%.2f", averageAccuracy) + "\n\n"; //write the average accuracy reading
            try {   //write file
                outputStream.write(fileContents.getBytes());    //write fileContents into file
                TV.setText(TV.getText() + "File Written");
            } catch (IOException e) {   //catch exception and print warning
                e.printStackTrace();
                TV.setText(TV.getText() + "\nERROR - File not written - IOException e");
                if (myToast != null) myToast.cancel();
                myToast = Toast.makeText(MapsActivity.this, "ERROR\nFile Not Written", Toast.LENGTH_SHORT);
                myToast.show();
            }
        }
        */
    }

    /* onMapReady:
     * "Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app."
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {   //sets up google maps
        gMap = googleMap;   //set gMap to the 'inputted' googleMap
        if (useSatellite != null && useSatellite) {
            gMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
        } else {
            gMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        }

        Criteria criteria = new Criteria(); //not completely sure how this works, but it does

        // draw field polygon
        if (SelectField.edges != null && SelectField.edges.size() > 2) {
            drawField();
        } else {
            //get last known location
            Location lastLocation = getBestLastLocation();

            if (lastLocation != null) {
                //zoom in camera on last known location when app is initialized
                gMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude()), 15));
                zoomed = true;  //camera has now been zoomed, does not need to happen again
            } else {
                //if no last-known location, then center map over arbitrary location without zooming
                gMap.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(37.6913, 262.6503)));
                zoomed = false;
            }
        }
    }

    private Location getBestLastLocation() {
        LocationManager mLocationManager = (LocationManager) getApplicationContext().getSystemService(LOCATION_SERVICE);
        List<String> providers = mLocationManager.getProviders(true);
        Location bestLocation = null;
        for (String provider : providers) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED
                    && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                getPermissionsCOARSEandFINE();
            }
            Location lastLocation = null;
            try {
                lastLocation = mLocationManager.getLastKnownLocation(provider);
            }catch (Exception ignored){}

            if (lastLocation == null) {
                continue;
            }
            if (bestLocation == null || lastLocation.getAccuracy() < bestLocation.getAccuracy()) {
                // Found best last known location: %s", l);
                bestLocation = lastLocation;
            }
        }
        return bestLocation;
    }

    private void drawField(){
        if(SelectField.edges != null && SelectField.edges.size() > 2) {
            Polygon field = gMap.addPolygon(new PolygonOptions()
                    .clickable(false)
                    .addAll(SelectField.edges)
            );
            field.setStrokeWidth(POLYGON_STROKE_WIDTH_PX);
            field.setStrokeColor(COLOR_BLACK_ARGB);
            field.setFillColor(COLOR_WHITE_ARGB);

            final LatLngBounds latLngBounds = getPolygonLatLngBounds(SelectField.edges);
            gMap.moveCamera(CameraUpdateFactory.newLatLngBounds(latLngBounds, POLYGON_PADDING_PREFERENCE));
            zoomed = true;
        }
    }

    private static LatLngBounds getPolygonLatLngBounds(final List<LatLng> polygon) {
        final LatLngBounds.Builder centerBuilder = LatLngBounds.builder();
        for (LatLng point : polygon) {
            centerBuilder.include(point);
        }
        return centerBuilder.build();
    }

    public void locationDetails() { //chooses which location function to use based on user's choice on location method

        if(on) { //ensures the program is "on"
            if(useFusedLocation != null && useFusedLocation.booleanValue()){
                FusedLocationDetails();
            }
            else {
                GPSLocationDetails();
            }
        }
    }

    public void drawPolygon(){

        if(positionList.size()>1) { // draw polyline unless only 1 LatLng point had been taken so far
            LatLng[] corners = getPolygonCorners(positionList.get(positionList.size() - 1), positionList.get(positionList.size() - 2));
            Polygon polygon1 = gMap.addPolygon(new PolygonOptions()
                    .clickable(false)
                    .add(corners[0], corners[1], corners[2], corners[3])
            );
            // TODO: style the polygon

            polygon1.setStrokeWidth(POLYGON_STROKE_WIDTH_PX);
            polygon1.setStrokeColor(COLOR_ORANGE_ARGB);
            polygon1.setFillColor(COLOR_ORANGE_ARGB);
        }

    }

    public LatLng[] getPolygonCorners(LatLng startLatLng, LatLng endLatLng){

        LatLng[] corners = new LatLng[4];   // initialize array to hold corner LatLng values

        // Picture a line going from startLatLng to endLatLng.
        // The angle going clockwise from North to the line is theta.
        /*
              North      end
                |        /
                |      /
                |..../
                |  /
                |/
              start
         */
        double theta = computeHeading(startLatLng, endLatLng);
        double phi = 90-theta;

        double distance = (pathWidth/2) * METERS_PER_FEET;   // distance from location point to edge of polygon
        // define the corners (see the comment block at the bottom of function for reference)
        corners[0] = computeOffsetOrigin(startLatLng, distance, 360-phi);   // point D
        corners[1] = computeOffsetOrigin(startLatLng, distance, 180-phi);   // point C
        corners[2] = computeOffsetOrigin(endLatLng, distance, 180-phi);     // point B
        corners[3] = computeOffsetOrigin(endLatLng, distance, 360-phi);     // point A

        return corners;

        /*
        Use this rectangle (ABCD) as reference for the code above

                   A      end
                          /
                        /        B
                      /
           D        /
                  /
              start     C

         */

    }


    public void createOffset(){
        /* Purpose: offsets the last value that was added to postionList by "offset" amount in ft
            If only 2 items in list, then must offset both. If only 1 item, need to wait - do nothing.
         */

        // if we only have one loation, can't get bearing to compute offset angle, so do nothing
        if(positionList.size() == 1) return;

        // compute offset in meters (TODO: should probably make this a one-time event)
        double offsetMeters = offset * METERS_PER_FEET;

        // get current location and previous locations
        LatLng start = positionList.get(positionList.size() - 2);
        LatLng end = positionList.get(positionList.size() - 1);

        if(positionList.size() == 2){   // if we just got first two points, need to offset both
            double phi = 90-computeHeading(start, end);
            // offset start and end values
            start = computeOffsetOrigin(start, offsetMeters, 180-phi);
            end = computeOffsetOrigin(end, offsetMeters, 180-phi);
            // replace LatLng values in the list with the offset ones
            positionList.set(positionList.size() - 2, start);
            positionList.set(positionList.size() - 1, end);

        }else if(positionList.size() > 2){
            LatLng oldStart = positionList.get(positionList.size() - 3);    // get value from 3 locations ago
            double oldPhi = 90-computeHeading(oldStart, start);             // get old phi
            // un-offset the start value so that we can get the proper bearing between last and current location
            LatLng originalStart = computeOffsetOrigin(start, offsetMeters, 360-oldPhi);
            // get current value of phi using un-offsetted "start" and pre-offset "end"
            double phi = 90-computeHeading(originalStart, end);
            end = computeOffsetOrigin(end, offsetMeters, 180-phi);  // offset "end"
            positionList.set(positionList.size() - 1, end); // add offsetted "end" to list

        }

    }
/********************************************************************
 *Functions for when using GPS only:
 ********************************************************************/

    void setCriteria(){ //sets criteria for GPS signal

        //specify that we want very high accuracy for GPS location reading
        locationCriteria.setAccuracy(Criteria.ACCURACY_FINE);
        locationCriteria.setPowerRequirement(Criteria.POWER_HIGH);
        locationCriteria.setAltitudeRequired(false);
        locationCriteria.setSpeedRequired(false);
        locationCriteria.setCostAllowed(true);
        locationCriteria.setBearingRequired(false);
        locationCriteria.setHorizontalAccuracy(Criteria.ACCURACY_HIGH);
        locationCriteria.setVerticalAccuracy(Criteria.ACCURACY_HIGH);
    }

    //for use when using GPS only
    void GPSLocationDetails(){  //reads lat lng values, calls drawPolyline() to update Polyline
        locationListener = new LocationListener() { //setting up new location listener
            @Override
            public void onLocationChanged(Location location) {
                //when location changes, display accuracy of that reading
                //currentLocation = location;
                if (on) {
                    currentLocation = location; //set current location
                    if(!setStartTime) { // if start time not yet set
                        //convert epoch time to calendar data
                        cal.setTimeInMillis(currentLocation.getTime()); //get time and put into calendar
                        time = dateFormatDayAndTime.format(cal.getTime());  //format date and time and set to string time
                        setStartTime = true;    //start time is now set
                    }
                    if(!zoomed){   //if map was not previously zoomed in, zoom it in now on current location

                        gMap.animateCamera(CameraUpdateFactory.zoomTo(gMap.getMaxZoomLevel()));
                        zoomed = true;  //camera is now zoomed
                    }
                    //get lat and long
                    currentLongitude = location.getLongitude();
                    currentLatitude = location.getLatitude();

                    //set lat and long into LatLng type variable
                    positionList.add(new LatLng(currentLatitude, currentLongitude));
                    if (offset != 1) createOffset();    //account for offset

                    //display values on screen
                    TV.setText(R.string.running);

                    //get time stamp
                    timeList.add(dateFormatTime.format(System.currentTimeMillis()));

                    drawPolygon();  //draw polygon on map

                    //update camera position
                    gMap.moveCamera(CameraUpdateFactory.newLatLng(positionList.getLast()));

                }
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {
                //not used right now
            }

            @Override
            public void onProviderEnabled(String provider) {
                //not used right now
            }

            @Override
            public void onProviderDisabled(String provider) {
                if(myToast != null) myToast.cancel();
                myToast = Toast.makeText(MapsActivity.this, "Error: GPS Provider Disabled", Toast.LENGTH_SHORT);
                myToast.show();
            }
        };
        //if permission is needed, get it
        if (!locationPermissionGranted) getPermissionsFINE();   //ensures that this only executes once after permission is granted

    }

    public void getPermissionsFINE(){   //gets permission to ACCESS_FINE_LOCATION and requests location updates
        //if at least Marshmallow, need to ask user's permission to get GPS data
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            //if permission is not yet granted, ask for it
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 0);
                if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    //if permission still not granted, tell user app will not work without it
                    if(myToast != null) myToast.cancel();
                    myToast = Toast.makeText(MapsActivity.this, "Need GPS permissions for app to function", Toast.LENGTH_SHORT);
                    myToast.show();
                }
                else{
                    locationManager.requestLocationUpdates(interval, 0, locationCriteria, locationListener, null);
                    locationPermissionGranted = true;
                }
            }
            else {
                locationManager.requestLocationUpdates(interval, 0, locationCriteria, locationListener, null);
                locationPermissionGranted = true;
            }

        }   //else if below Marshmallow, we don't need to ask special permission
        else if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
            assert locationManager != null;
            locationManager.requestLocationUpdates(interval, 0, locationCriteria, locationListener, null);
            locationPermissionGranted = true;
        }
        else{
            //if permission still not granted, tell user app will not work without it
            if(myToast != null) myToast.cancel();
            myToast = Toast.makeText(MapsActivity.this, "Need GPS permissions for app to function", Toast.LENGTH_SHORT);
            myToast.show();
        }
    }



/********************************************************************
 *Functions for when using Fused Location Services:
 ********************************************************************/

    //for use when using Fused Loc Serv
    void FusedLocationDetails(){    //calls functions to create a location request, get permissions, and request location updates (which takes care of mapping, etc.

        createLocationRequest(); //specify params for fused location request

        //if permission is needed, get it
        //and request location updates
        if (!locationPermissionGranted) {//ensures that this only executes once after permission is granted
            getPermissionsCOARSEandFINE();
        }
    }

    //setting parameters for fused location requests (if applicable)
    private void createLocationRequest()
    {  //see explanations on   https://github.com/codepath/android_guides/wiki/Retrieving-Location-with-LocationServices-API

        myLocationRequest= new LocationRequest();   //setup new location request to be used when requesting location updates
        myLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);  //want highest accuracy possible
        //setting approx, max, and min update intervals to the same value
        myLocationRequest.setInterval(interval);    //set approx update interval
        myLocationRequest.setFastestInterval(interval); //set max update interval
        myLocationRequest.setMaxWaitTime(interval);     //set min update interval

        // Create LocationSettingsRequest object using location request
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
        builder.addLocationRequest(myLocationRequest);
        LocationSettingsRequest locationSettingsRequest = builder.build();

        // Check whether location settings are satisfied
        // https://developers.google.com/android/reference/com/google/android/gms/location/SettingsClient
        SettingsClient settingsClient = LocationServices.getSettingsClient(this);
        settingsClient.checkLocationSettings(locationSettingsRequest);

    }

    void getPermissionsCOARSEandFINE(){ //gets permission to ACCESS_FINE_LOCATION and ACCESS_COARSE_LOCATION and then requests location updates
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            //check to see if we have permissions, if not try to get them
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 0);
                requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 0);

                if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    //if permission still not granted, tell user app will not work without it
                    if(myToast != null) myToast.cancel();
                    myToast = Toast.makeText(MapsActivity.this, "Need location permissions for app to function", Toast.LENGTH_SHORT);
                    myToast.show();
                }
                //once permission is granted, set up location listener
                else{
                    myFusedLocationClient.requestLocationUpdates(myLocationRequest, createLocationCallback(), null);    //request updates
                    locationPermissionGranted = true;
                }
            }
            else {
                myFusedLocationClient.requestLocationUpdates(myLocationRequest, createLocationCallback(), null);    //request updates
                locationPermissionGranted = true;
            }

        }   //else if below Marshmallow, we don't need to ask special permission
        else if(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED){
            myFusedLocationClient.requestLocationUpdates(myLocationRequest, createLocationCallback(), null);    //request updates
            locationPermissionGranted = true;
        }
        else{
            //if permission still not granted, tell user app will not work without it
            if(myToast != null) myToast.cancel();
            myToast = Toast.makeText(MapsActivity.this, "Need location permissions for app to function", Toast.LENGTH_SHORT);
            myToast.show();
        }
        //in future, could make app revert to only use GPS if ACCESS_FINE_LOCATION is granted but not ACCESS_COARSE_LOCATION
    }


    LocationCallback createLocationCallback(){
        //creates and returns a location callback. Location Callback is needed when requesting updates using Fused Location Provider
        //also performs all of the mapping functions
        myLocationCallback = new LocationCallback(){    //create new location callback
            @Override
            public void onLocationResult(LocationResult locationResult) {
                //when location changes, display accuracy of that reading
                //currentLocation = location;
                if (on) {
                    currentLocationResult = locationResult; //set current location
                    if(!setStartTime) { // if start time not yet set
                        //convert epoch time to calendar data
                        cal.setTimeInMillis(currentLocationResult.getLastLocation().getTime()); //get time and put into calendar
                        time = dateFormatDayAndTime.format(cal.getTime());  //format date and time and set to string time
                        setStartTime = true;    //start time is now set
                    }
                    if(!zoomed){   //if map was not previously zoomed in, zoom it in now on current location

                        gMap.animateCamera(CameraUpdateFactory.zoomTo(gMap.getMaxZoomLevel()));
                        zoomed = true;  //camera is now zoomed
                    }
                    //get lat and long
                    currentLongitude = currentLocationResult.getLastLocation().getLongitude();
                    currentLatitude = currentLocationResult.getLastLocation().getLatitude();

                    //set lat and long into LatLng type variable
                    positionList.add(new LatLng(currentLatitude, currentLongitude));
                    if (offset != 1) createOffset();    //account for offset

                    //display values on screen
                    TV.setText(R.string.running);

                    //get time stamp
                    timeList.add(dateFormatTime.format(System.currentTimeMillis()));

                    drawPolygon();  //add polygon to map

                    //update camera position
                    gMap.moveCamera(CameraUpdateFactory.newLatLng(positionList.getLast()));

                }
            }
        };
        return myLocationCallback;
    }



/* *******************************************************************
 *Notes:
 ********************************************************************/

    /*
    getAccuracy() function:
    From https://developer.android.com/reference/android/location/Location#getAccuracy()
    "We define horizontal accuracy as the radius of 68% confidence.
    In other words, if you draw a circle centered at this location's latitude and longitude,
    and with a radius equal to the accuracy, then there is a 68% probability that the true
    location is inside the circle. This accuracy estimation is only concerned with horizontal
    accuracy, and does not indicate the accuracy of bearing, velocity or altitude if those are
    included in this Location. If this location does not have a horizontal accuracy, then 0.0
    is returned. All locations generated by the LocationManager include horizontal accuracy."
     */

}
