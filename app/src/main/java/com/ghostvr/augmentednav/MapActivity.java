package com.ghostvr.augmentednav;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.PointF;
import android.location.Location;
import android.location.LocationManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import com.here.android.mpa.common.GeoBoundingBox;
import com.here.android.mpa.common.GeoCoordinate;
import com.here.android.mpa.common.OnEngineInitListener;
import com.here.android.mpa.common.PositioningManager;
import com.here.android.mpa.common.ViewObject;
import com.here.android.mpa.mapping.Map;
import com.here.android.mpa.mapping.MapFragment;
import com.here.android.mpa.mapping.MapGesture;
import com.here.android.mpa.mapping.MapMarker;
import com.here.android.mpa.mapping.MapRoute;
import com.here.android.mpa.routing.RouteManager;
import com.here.android.mpa.routing.RouteOptions;
import com.here.android.mpa.routing.RoutePlan;
import com.here.android.mpa.routing.RouteResult;

import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MapActivity extends AppCompatActivity {
    private Map map = null;
    private MapFragment mapFragment = null;
    private MapRoute mapRoute = null;


    private final static int REQUEST_CODE_ASK_PERMISSIONS = 1;
    private static final String[] REQUIRED_SDK_PERMISSIONS = new String[]{
            Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.WRITE_EXTERNAL_STORAGE};

    private TextView tv_map = null;
    private Button b_startNavigation = null;
    private EditText et_start_latitude = null;
    private EditText et_start_longitude = null;
    private EditText et_end_latitude = null;
    private EditText et_end_longitude = null;
    private RadioButton rb_start = null;
    private RadioButton rb_end = null;

    private double start_latitude;
    private double start_longitude;
    private double end_latitude;
    private double end_longitude;

    private RouteManager.Listener routeManagerListener =
            new RouteManager.Listener() {
                public void onCalculateRouteFinished(RouteManager.Error errorCode,
                                                     List<RouteResult> result) {

                    if (errorCode == RouteManager.Error.NONE && result.get(0).getRoute() != null) {

                        final List<Location> locationList = new ArrayList<>();
                        List<GeoCoordinate> routeCoordinates = result.get(0).getRoute().getRouteGeometry();

                        Location l1 = new Location("location");
                        l1.setLatitude(routeCoordinates.get(0).getLatitude());
                        l1.setLongitude(routeCoordinates.get(0).getLongitude());
                        locationList.add(l1);

                        for (int i = 1; i < routeCoordinates.size(); ++i) {
                            if (routeCoordinates.get(i).distanceTo(routeCoordinates.get(i - 1)) > 10) {
                                Location location = new Location("location");
                                location.setLatitude(routeCoordinates.get(i).getLatitude());
                                location.setLongitude(routeCoordinates.get(i).getLongitude());
                                locationList.add(location);
                            }
                        }

                        for (Location l : locationList)
                            Log.d("Route coordinates", l.getLatitude() + "\t" + l.getLongitude());

                        // create a map route object and place it on the map
                        mapRoute = new MapRoute(result.get(0).getRoute());
                        map.addMapObject(mapRoute);

                        // Get the bounding box containing the route and zoom in
                        GeoBoundingBox gbb = result.get(0).getRoute().getBoundingBox();
                        map.zoomTo(gbb, Map.Animation.NONE,
                                Map.MOVE_PRESERVE_ORIENTATION);

                        tv_map.setText(String.format("Route calculated with %d maneuvers.",
                                result.get(0).getRoute().getManeuvers().size()));

                        b_startNavigation.setVisibility(View.VISIBLE);
                        b_startNavigation.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                Intent intent = new Intent(MapActivity.this, NavigationActivity.class);
                                intent.putExtra("locationList", (Serializable) locationList);
                                startActivity(intent);
                            }
                        });
                    } else {
                        tv_map.setText(String.format("Route calculation failed: %s",
                                errorCode.toString()));
                        b_startNavigation.setVisibility(View.INVISIBLE);
                    }
                }

                public void onProgress(int percentage) {
                    tv_map.setText(String.format("... %d percent done ...", percentage));
                }
            };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        checkPermissions();
    }

    public void getDirections(View view) {

        if (validateInput()) {
            // 1. clear previous results
            tv_map.setText("");
            if (map != null && mapRoute != null) {
                map.removeMapObject(mapRoute);
                mapRoute = null;
            }
            // 2. Initialize RouteManager
            RouteManager routeManager = new RouteManager();

            // 3. Select routing options via RoutingMode
            RoutePlan routePlan = new RoutePlan();

            RouteOptions routeOptions = new RouteOptions();
            routeOptions.setTransportMode(RouteOptions.TransportMode.CAR);
            routeOptions.setRouteType(RouteOptions.Type.FASTEST);
            routePlan.setRouteOptions(routeOptions);

            // 4. Select Way-points for your routes
            // Start Point
            routePlan.addWaypoint(new GeoCoordinate(start_latitude, start_longitude));

            // End Point
            routePlan.addWaypoint(new GeoCoordinate(end_latitude, end_longitude));

            // 5. Retrieve Routing information via RouteManagerListener
            RouteManager.Error error =
                    routeManager.calculateRoute(routePlan, routeManagerListener);
            if (error != RouteManager.Error.NONE) {
                Toast.makeText(getApplicationContext(),
                        "Route calculation failed with: " + error.toString(),
                        Toast.LENGTH_SHORT)
                        .show();
            }
        } else {
            tv_map.setText("Invalid data");
        }
    }

    private boolean validateInput() {
        boolean isValidInput = true;

        try {
            start_latitude = Double.parseDouble(et_start_latitude.getText().toString());
            start_longitude = Double.parseDouble(et_start_longitude.getText().toString());
            end_latitude = Double.parseDouble(et_end_latitude.getText().toString());
            end_longitude = Double.parseDouble(et_end_longitude.getText().toString());
        } catch (Exception e) {
            isValidInput = false;
        }
        return isValidInput;
    }


    //Checks the dynamically-controlled permissions and requests missing permissions from end user.
    protected void checkPermissions() {
        final List<String> missingPermissions = new ArrayList<>();
        // check all required dynamic permissions
        for (final String permission : REQUIRED_SDK_PERMISSIONS) {
            final int result = ContextCompat.checkSelfPermission(this, permission);
            if (result != PackageManager.PERMISSION_GRANTED) {
                missingPermissions.add(permission);
            }
        }
        if (!missingPermissions.isEmpty()) {
            // request all missing permissions
            final String[] permissions = missingPermissions
                    .toArray(new String[missingPermissions.size()]);
            ActivityCompat.requestPermissions(this, permissions, REQUEST_CODE_ASK_PERMISSIONS);
        } else {
            final int[] grantResults = new int[REQUIRED_SDK_PERMISSIONS.length];
            Arrays.fill(grantResults, PackageManager.PERMISSION_GRANTED);
            onRequestPermissionsResult(REQUEST_CODE_ASK_PERMISSIONS, REQUIRED_SDK_PERMISSIONS,
                    grantResults);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CODE_ASK_PERMISSIONS:
                for (int index = permissions.length - 1; index >= 0; --index) {
                    if (grantResults[index] != PackageManager.PERMISSION_GRANTED) {
                        // exit the app if one permission is not granted
                        Toast.makeText(this, "Required permission '" + permissions[index]
                                + "' not granted, exiting", Toast.LENGTH_LONG).show();
                        finish();
                        return;
                    }
                }
                // all permissions were granted
                initialize();
                break;
        }
    }

    private void initialize() {
        setContentView(R.layout.activity_map);

        tv_map = (TextView) findViewById(R.id.tv_map);
        b_startNavigation = (Button) findViewById(R.id.b_startNavigation);
        et_start_latitude = (EditText) findViewById(R.id.et_start_latitude);
        et_start_longitude = (EditText) findViewById(R.id.et_start_longitude);
        et_end_latitude = (EditText) findViewById(R.id.et_end_latitude);
        et_end_longitude = (EditText) findViewById(R.id.et_end_longitude);
        rb_start = (RadioButton) findViewById(R.id.rb_start);
        rb_end = (RadioButton) findViewById(R.id.rb_end);

        rb_start.setChecked(true);

        rb_start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                rb_end.setChecked(false);
            }
        });

        rb_end.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                rb_start.setChecked(false);
            }
        });

        // Search for the map fragment to finish setup by calling init().
        mapFragment = (MapFragment) getFragmentManager().findFragmentById(
                R.id.mapfragment);
        mapFragment.init(new OnEngineInitListener() {
            @Override
            public void onEngineInitializationCompleted(
                    OnEngineInitListener.Error error) {
                if (error == OnEngineInitListener.Error.NONE) {
                    // retrieve a reference of the map from the map fragment
                    map = mapFragment.getMap();
                    // Set the map center to the Vancouver region (no animation)
                    map.setCenter(new GeoCoordinate(12.990323, 80.233413, 0.0),
                            Map.Animation.NONE);
                    // Set the zoom level to the average between min and max
                    map.setZoomLevel(15.4);

                    LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
                    if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        return;
                    }
                    Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                    MapMarker mapMarker = new MapMarker();
                    mapMarker.setCoordinate(new GeoCoordinate(location.getLatitude(), location.getLongitude()));
                    map.addMapObject(mapMarker);

                    mapFragment.getMapGesture().addOnGestureListener(new MapGesture.OnGestureListener() {
                        @Override
                        public void onPanStart() {

                        }

                        @Override
                        public void onPanEnd() {

                        }

                        @Override
                        public void onMultiFingerManipulationStart() {

                        }

                        @Override
                        public void onMultiFingerManipulationEnd() {

                        }

                        @Override
                        public boolean onMapObjectsSelected(List<ViewObject> list) {
                            return false;
                        }

                        @Override
                        public boolean onTapEvent(PointF pointF) {
                            return false;
                        }

                        @Override
                        public boolean onDoubleTapEvent(PointF pointF) {
                            return false;
                        }

                        @Override
                        public void onPinchLocked() {

                        }

                        @Override
                        public boolean onPinchZoomEvent(float v, PointF pointF) {
                            return false;
                        }

                        @Override
                        public void onRotateLocked() {

                        }

                        @Override
                        public boolean onRotateEvent(float v) {
                            return false;
                        }

                        @Override
                        public boolean onTiltEvent(float v) {
                            return false;
                        }

                        @Override
                        public boolean onLongPressEvent(PointF pointF) {
                            GeoCoordinate geoCoordinate = map.pixelToGeo(pointF);

                            if (rb_start.isChecked()){
                                et_start_latitude.setText(geoCoordinate.getLatitude() + "");
                                et_start_longitude.setText(geoCoordinate.getLongitude() + "");
                            } else {
                                et_end_latitude.setText(geoCoordinate.getLatitude() + "");
                                et_end_longitude.setText(geoCoordinate.getLongitude() + "");
                            }
                            return false;
                        }

                        @Override
                        public void onLongPressRelease() {

                        }

                        @Override
                        public boolean onTwoFingerTapEvent(PointF pointF) {
                            return false;
                        }
                    });
                } else {
                    System.out.println("ERROR: Cannot initialize Map Fragment");
                }
            }
        });
    }
}
