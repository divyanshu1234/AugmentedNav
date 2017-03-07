package com.ghostvr.augmentednav;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.GeomagneticField;
import android.location.Location;
import com.google.android.gms.location.LocationListener;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.*;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Divyanshu on 3/8/17.
 */

public class NavigationActivity extends AppCompatActivity implements
        ConnectionCallbacks, OnConnectionFailedListener, LocationListener {

    private CustomGLSurfaceView glSurfaceView;
    private boolean rendererSet = false;

    private static List<Location> locationList;

    private static int index = 0;
    private double dx, dy;

    private static TextView tv_location_data_1;
    private static TextView tv_location_data_2;

    GoogleApiClient mGoogleApiClient;
    LocationRequest mLocationRequest;
    Boolean mRequestingLocationUpdates = true;
    Location mCurrentLocation;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_navigation);

        glSurfaceView = (CustomGLSurfaceView) findViewById(R.id.gl_surface_view);
        rendererSet = glSurfaceView.isRendererSet();

        tv_location_data_1 = (TextView) findViewById(R.id.tv_location_data_1);
        tv_location_data_2 = (TextView) findViewById(R.id.tv_location_data_2);

        locationList = (ArrayList<Location>)getIntent().getSerializableExtra("locationList");

        buildGoogleApiClient();
    }


    private synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();

        createLocationRequest();
    }

    protected void createLocationRequest() {
        mLocationRequest = new LocationRequest();

        mLocationRequest.setInterval(10);
        mLocationRequest.setFastestInterval(5);
        mLocationRequest.setSmallestDisplacement(2f);

        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    protected void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(
                mGoogleApiClient, mLocationRequest, this);
    }

    protected void stopLocationUpdates() {
        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
    }


    private static void setLocationData1(Location location) {
        String nextPointText;
        if (index == 6)
            nextPointText = "Reached";
        else
            nextPointText = locationList.get(index).getLatitude()
                    + "\t\t" + locationList.get(index).getLongitude()
                    + "\n" + location.distanceTo(locationList.get(index))
                    + "\t\tindex = " + index;

        tv_location_data_1.setText(location.getLatitude()
                + "\t\t" + location.getLongitude()
                + "\n" + nextPointText
                + "\nTotal Points = " + locationList.size());
        Log.d("Location", location.getLatitude() + "\t\t" + location.getLongitude() + "\n" + nextPointText);
    }

    private float calcDirection(Location uLocation) {

        if (uLocation.distanceTo(locationList.get(index)) < 8)
            ++index;

        if (index == 6)
            return 0f;

        double nLatitude = locationList.get(index).getLatitude();
        double nLongitude = locationList.get(index).getLongitude();

        dy = nLatitude - uLocation.getLatitude();
        dx = nLongitude - uLocation.getLongitude();



        if (dy == 0) {
            if (dx == 0)
                return 0f;
            return dx > 0 ? (float) (-Math.PI / 2) : (float) (Math.PI / 2);
        }

        if (dx == 0) {
            if (dy == 0)
                return 0f;
            return dy > 0 ? 0f : (float) Math.PI;
        }

        if (dx > 0 && dy > 0)                               //First Quadrant
            return (float) -Math.atan2(dx, dy);

        if (dx < 0 && dy > 0)                               //Second Quadrant
            return (float) Math.atan2(-dx, dy);

        if (dx < 0 && dy < 0)                               //Third Quadrant
            return (float) (Math.PI - Math.atan2(-dx, -dy));

        else                                                //Fourth Quadrant
            return (float) (Math.atan2(dx, -dy) - Math.PI);
    }

    private float calcDirection2(Location uLocation) {
        if (uLocation.distanceTo(locationList.get(index)) < 8)
            ++index;

        if (index == 6)
            return 0f;

        return uLocation.bearingTo(locationList.get(index));
    }

    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (rendererSet) {
            glSurfaceView.onResume();
        }

        if (mGoogleApiClient.isConnected() && mRequestingLocationUpdates)
            startLocationUpdates();
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (rendererSet) {
            glSurfaceView.onPause();
        }

        if (mGoogleApiClient.isConnected())
            stopLocationUpdates();
    }

    @Override
    protected void onStop() {
        mGoogleApiClient.disconnect();
        super.onStop();
    }

    @Override
    public void onLocationChanged(Location location) {
        mCurrentLocation = location;
        setLocationData1(location);

        float angleInDegrees = calcDirection(location) * 180 / (float) Math.PI;

//        float angleInDegrees = calcDirection2(location);

        GeomagneticField geoField = new GeomagneticField(
                (float) location.getLatitude(),
                (float) location.getLongitude(),
                (float) location.getAltitude(),
                System.currentTimeMillis());

//        angleInDegrees += geoField.getDeclination();

        tv_location_data_2.setText("dx = " + dx + "\ndy = " + dy + "\nangle = " + angleInDegrees);

        glSurfaceView.setAngle(angleInDegrees);
    }


    @Override
    public void onConnected(@Nullable Bundle bundle) {
        if (mCurrentLocation == null) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            mCurrentLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        }

        if (mRequestingLocationUpdates) {
            startLocationUpdates();
        }

    }

    @Override
    public void onConnectionSuspended(int i) {
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }
}