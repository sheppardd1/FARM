package com.example.davea.FARM;

import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.service.autofill.TextValueSanitizer;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.maps.android.SphericalUtil;

import java.text.DecimalFormat;
import java.util.LinkedList;
import java.util.List;

public class SelectField extends FragmentActivity implements OnMapReadyCallback {

    // polygon formatting
    private static final int COLOR_BLACK_ARGB = 0x7f000000;
    private static final int COLOR_WHITE_ARGB = 0x3fffffff;
    private static final int COLOR_GREEN_ARGB = 0x7f388E3C;
    private static final int COLOR_PURPLE_ARGB = 0x7f81C784;
    private static final int COLOR_ORANGE_ARGB = 0x7fF57F17;
    private static final int COLOR_BLUE_ARGB = 0x7fF9A825;

    private static final int POLYGON_STROKE_WIDTH_PX = 2;
    private static final int PATTERN_DASH_LENGTH_PX = 20;
    private static final int PATTERN_GAP_LENGTH_PX = 20;

    TextView TV;
    Button done, undo;
    static LinkedList<LatLng> edges = new LinkedList<>();
    LinkedList<Marker> markers = new LinkedList<>();
    Toast myToast = null;

    Polygon field;
    GoogleMap gMap;

    DecimalFormat decimalFormat = new DecimalFormat("#.00");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_field);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        assert mapFragment != null;
        mapFragment.getMapAsync(this);

        TV = findViewById(R.id.TV);
        TV.setText(R.string.drawField);
        done = findViewById(R.id.btnDone);
        undo = findViewById(R.id.btnUndo);
        edges.clear();


        done.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // if not enough edges for polygon but more than 0
                if(edges != null && edges.size() > 0 && edges.size() < 3) {
                    if (myToast != null) myToast.cancel();
                    myToast = Toast.makeText(SelectField.this, R.string.drawFieldError, Toast.LENGTH_SHORT);
                    myToast.show();
                }
                else{
                    startActivity(new Intent(getApplicationContext(), MapsActivity.class));    //go to map boundary setting activity
                }

            }
        });

        undo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(edges != null && edges.size()>0) {
                    edges.removeLast();
                    markers.getLast().remove();
                    markers.removeLast();
                    drawPolygon();
                }
            }
        });

        TV.setText(R.string.drawFieldInstructions);

    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        gMap = googleMap;
        // setup map
        if(MapsActivity.useSatellite) {
            gMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
        }else{
            gMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        }

        //get last known location
        Location lastLocation = getBestLastLocation();

        if (lastLocation != null) {
            //zoom in camera on last known location when app is initialized
            gMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude()), 15));
        } else {
            //if no last-known location, then center map over arbitrary location without zooming
            gMap.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(37.6913, 262.6503)));
        }

        gMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng point) {
                markers.add(gMap.addMarker(new MarkerOptions()
                        .position(point)
                ));
                edges.add(point);
                drawPolygon();
            }
        });

    }

    private Location getBestLastLocation() {
        // try to get last location. If user has not granted necessary permissions, don't bother

        LocationManager mLocationManager = (LocationManager) getApplicationContext().getSystemService(LOCATION_SERVICE);
        assert mLocationManager != null;
        List<String> providers = mLocationManager.getProviders(true);
        Location bestLocation = null;
        for (String provider : providers) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED
                    && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {

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

        }
        return bestLocation;
    }


    public void drawPolygon(){

        // if there is a polygon already up, remove it
        if (field != null){
            field.remove();
        }

        // if there are enough points to make a polygon, make one
        if(edges != null && edges.size() > 2){
            field = gMap.addPolygon(new PolygonOptions()
                    .clickable(false)
                    .addAll(edges)
            );
            field.setStrokeWidth(POLYGON_STROKE_WIDTH_PX);
            field.setStrokeColor(COLOR_BLACK_ARGB);
            field.setFillColor(COLOR_WHITE_ARGB);
            double area = SphericalUtil.computeArea(edges);
            area *= 0.00024711; //convert to acres
            String acre_string = decimalFormat.format(area);
            TV.setText("Total Area: " + acre_string + " acres");
        }
        else if (edges != null && edges.size() < 3){
            TV.setText(R.string.drawFieldInstructions);
        }


    }




}
