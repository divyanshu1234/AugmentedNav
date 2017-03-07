package com.ghostvr.augmentednav;

import android.Manifest;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.here.android.mpa.common.GeoBoundingBox;
import com.here.android.mpa.common.GeoCoordinate;
import com.here.android.mpa.common.OnEngineInitListener;
import com.here.android.mpa.mapping.Map;
import com.here.android.mpa.mapping.MapFragment;
import com.here.android.mpa.mapping.MapRoute;
import com.here.android.mpa.routing.RouteManager;
import com.here.android.mpa.routing.RouteOptions;
import com.here.android.mpa.routing.RoutePlan;
import com.here.android.mpa.routing.RouteResult;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private Map map = null;
    private MapFragment mapFragment = null;
    private MapRoute mapRoute = null;


    private final static int REQUEST_CODE_ASK_PERMISSIONS = 1;
    private static final String[] REQUIRED_SDK_PERMISSIONS = new String[] {
            Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.WRITE_EXTERNAL_STORAGE };

    private TextView tv_map = null;
    private EditText et_start_latitude = null;
    private EditText et_start_longitude = null;
    private EditText et_end_latitude = null;
    private EditText et_end_longitude = null;

    private double start_latitude;
    private double start_longitude;
    private double end_latitude;
    private double end_longitude;

    private RouteManager.Listener routeManagerListener =
            new RouteManager.Listener()
            {
                public void onCalculateRouteFinished(RouteManager.Error errorCode,
                                                     List<RouteResult> result) {

                    if (errorCode == RouteManager.Error.NONE && result.get(0).getRoute() != null) {

                        List<GeoCoordinate> routeCoordinates = result.get(0).getRoute().getRouteGeometry();
                        for (GeoCoordinate coordinate : routeCoordinates) {
                            Log.d("Route Coordinate:", coordinate.getLatitude() + "\t" + coordinate.getLongitude());
                        }
                        // create a map route object and place it on the map
                        mapRoute = new MapRoute(result.get(0).getRoute());
                        map.addMapObject(mapRoute);

                        // Get the bounding box containing the route and zoom in
                        GeoBoundingBox gbb = result.get(0).getRoute().getBoundingBox();
                        map.zoomTo(gbb, Map.Animation.NONE,
                                Map.MOVE_PRESERVE_ORIENTATION);

                        tv_map.setText(String.format("Route calculated with %d maneuvers.",
                                result.get(0).getRoute().getManeuvers().size()));
                    } else {
                        tv_map.setText(String.format("Route calculation failed: %s",
                                errorCode.toString()));
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

        if (validateInput()){
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

            // 4. Select Waypoints for your routes
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
        } else{
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
        } catch (Exception e){
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
        setContentView(R.layout.activity_main);

        tv_map = (TextView) findViewById(R.id.tv_map);

        et_start_latitude = (EditText) findViewById(R.id.et_start_latitude);
        et_start_longitude = (EditText) findViewById(R.id.et_start_longitude);
        et_end_latitude = (EditText) findViewById(R.id.et_end_latitude);
        et_end_longitude = (EditText) findViewById(R.id.et_end_longitude);

        // Search for the map fragment to finish setup by calling init().
        mapFragment = (MapFragment)getFragmentManager().findFragmentById(
                R.id.mapfragment);
        mapFragment.init(new OnEngineInitListener() {
            @Override
            public void onEngineInitializationCompleted(
                    OnEngineInitListener.Error error)
            {
                if (error == OnEngineInitListener.Error.NONE) {
                    // retrieve a reference of the map from the map fragment
                    map = mapFragment.getMap();
                    // Set the map center to the Vancouver region (no animation)
                    map.setCenter(new GeoCoordinate(12.995532, 80.239312, 0.0),
                            Map.Animation.NONE);
                    // Set the zoom level to the average between min and max
                    map.setZoomLevel(
                            (map.getMaxZoomLevel() + map.getMinZoomLevel()) / 2);
                } else {
                    System.out.println("ERROR: Cannot initialize Map Fragment");
                }
            }
        });
    }
}
