package com.ghostvr.augmentednav;

import android.Manifest;
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
    private static int totalPoints = 0;

    private static int minDistIndex;
    private static int nextPointIndex = 0;
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
        totalPoints = locationList.size();

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

    private void setIndex(Location location) {
        float minDist = location.distanceTo(locationList.get(0));
        minDistIndex = 0;

        for(int i = 1; i < totalPoints; ++i){
            if (location.distanceTo(locationList.get(i)) < minDist){
                minDist = location.distanceTo(locationList.get(i));
                minDistIndex = i;
            }
        }

        if (nextPointIndex != minDistIndex && nextPointIndex != minDistIndex + 1){
            nextPointIndex = minDistIndex;
        }

        if (location.distanceTo(locationList.get(nextPointIndex)) < 10){
            ++nextPointIndex;
        }
    }

    private static void setLocationDataText(Location location) {
        String nextPointText;
        if (nextPointIndex == totalPoints)
            nextPointText = "Reached";
        else
            nextPointText = locationList.get(nextPointIndex).getLatitude()
                    + "\t\t" + locationList.get(nextPointIndex).getLongitude()
                    + "\n" + location.distanceTo(locationList.get(nextPointIndex))
                    + "\t\tnextIndex = " + nextPointIndex
                    + "\tminIndex = " + minDistIndex;

        tv_location_data_1.setText(location.getLatitude()
                + "\t\t" + location.getLongitude()
                + "\n" + nextPointText
                + "\nTotal Points = " + totalPoints);
        Log.d("Location", location.getLatitude() + "\t\t" + location.getLongitude() + "\n" + nextPointText);
    }

    private float calcDirection(Location uLocation) {

        if (nextPointIndex == totalPoints)
            return 0f;

        double nLatitude = locationList.get(nextPointIndex).getLatitude();
        double nLongitude = locationList.get(nextPointIndex).getLongitude();

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


    @Override
    public void onLocationChanged(Location location) {
        mCurrentLocation = location;
        setIndex(location);
        setLocationDataText(location);

        float angleInDegrees = calcDirection(location) * 180 / (float) Math.PI;

        GeomagneticField geoField = new GeomagneticField(
                (float) location.getLatitude(),
                (float) location.getLongitude(),
                (float) location.getAltitude(),
                System.currentTimeMillis());

        angleInDegrees += geoField.getDeclination();

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
}