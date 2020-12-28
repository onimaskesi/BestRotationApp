package com.onimaskesi.bestrotationapp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Context;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.Toast;

import com.huawei.hmf.tasks.OnFailureListener;
import com.huawei.hmf.tasks.OnSuccessListener;
import com.huawei.hms.common.ApiException;
import com.huawei.hms.common.ResolvableApiException;
import com.huawei.hms.location.FusedLocationProviderClient;
import com.huawei.hms.location.LocationAvailability;
import com.huawei.hms.location.LocationCallback;
import com.huawei.hms.location.LocationRequest;
import com.huawei.hms.location.LocationResult;
import com.huawei.hms.location.LocationServices;
import com.huawei.hms.location.LocationSettingsRequest;
import com.huawei.hms.location.LocationSettingsResponse;
import com.huawei.hms.location.LocationSettingsStatusCodes;
import com.huawei.hms.location.SettingsClient;
import com.huawei.hms.maps.CameraUpdateFactory;
import com.huawei.hms.maps.HuaweiMap;
import com.huawei.hms.maps.MapView;
import com.huawei.hms.maps.MapsInitializer;
import com.huawei.hms.maps.OnMapReadyCallback;
import com.huawei.hms.maps.model.LatLng;
import com.huawei.hms.maps.model.MapStyleOptions;
import com.huawei.hms.maps.model.MarkerOptions;
import com.huawei.hms.maps.model.Polyline;
import com.huawei.hms.maps.model.PolylineOptions;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;


public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final String TAG = "bra";

    private static final String[] RUNTIME_PERMISSIONS = {Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.INTERNET};

    private static final int REQUEST_CODE = 100;

    //HUAWEI map
    private HuaweiMap hMap;
    private MapView mMapView;

    //Location
    private FusedLocationProviderClient fusedLocationProviderClient;
    private SettingsClient settingsClient;
    private LocationRequest mLocationRequest;
    private LocationCallback mLocationCallback;
    private Location mLocation;

    private String apiKey = "CgB6e3x9OgwV9lrubpb5P0yCfo4sh3XaTtuxdkmrXJfXsWEpX5a9x/ImEsf93/b4YLpIRyhe8bp9S7ngsZmAsFFw";

    private static final String MAPVIEW_BUNDLE_KEY = "MapViewBundleKey";

    private LatLng destination;

    private List<Road> Roads = new ArrayList<Road>();
    private Road drivingRoad = new Road();
    private Road walkingRoad = new Road();
    private Road bicyclingRoad = new Road();

    private Button modeButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //get mapview instance
        mMapView = findViewById(R.id.mapView);

        drivingRoad.serviceName = "driving";
        drivingRoad.color = Color.BLUE;

        walkingRoad.serviceName = "walking";
        walkingRoad.color = Color.GREEN;

        bicyclingRoad.serviceName = "bicycling";
        bicyclingRoad.color = Color.YELLOW;

        Roads.add(drivingRoad);
        Roads.add(walkingRoad);
        Roads.add(bicyclingRoad);

        if (!hasPermissions(this, RUNTIME_PERMISSIONS)) {
            ActivityCompat.requestPermissions(this, RUNTIME_PERMISSIONS, REQUEST_CODE);
        }

        Bundle mapViewBundle = null;

        if (savedInstanceState != null) {
            mapViewBundle = savedInstanceState.getBundle(MAPVIEW_BUNDLE_KEY);
        }

        MapsInitializer.setApiKey(apiKey);
        mMapView.onCreate(mapViewBundle);


        //get map instance
        mMapView.getMapAsync(this);
    }

    @Override
    public void onMapReady(HuaweiMap map) {
        //get map instance in a callback method
        Log.d(TAG, "onMapReady: ");
        hMap = map;

        modeButton = findViewById(R.id.modeButton);

        hMap.setMyLocationEnabled(true);
        hMap.getUiSettings().setMyLocationButtonEnabled(true);

        setNightStyle();

        destination = new LatLng( 41.028880,29.117224);
        hMap.addMarker(new MarkerOptions()
                .position(destination)
                .title("Huawei Türkiye Ofisi")
        );
        hMap.moveCamera(CameraUpdateFactory.newLatLngZoom( destination, 13));

        getLocation();
    }

    public void ModeButtonClick(View view){

        String text = modeButton.getText().toString();
        if(text.contains("Night Mod")){
            modeButton.setText("Retro Mode");
            modeButton.setBackgroundResource(R.drawable.retrom_button);
            modeButton.setTextColor(Color.parseColor("#FF9800"));
            setRetroStyle();
        }
        if(text.contains("Retro Mode")){
            modeButton.setText("Night Mode");
            modeButton.setBackgroundResource(R.drawable.nighm_button);
            modeButton.setTextColor(Color.parseColor("#FFFFFF"));
            setNightStyle();
        }

    }

    public void setRetroStyle() {
        MapStyleOptions styleOptions = MapStyleOptions.loadRawResourceStyle(this, R.raw.mapstyle_retro_hms);
        hMap.setMapStyle(styleOptions);
    }

    public void setNightStyle() {
        MapStyleOptions styleOptions = MapStyleOptions.loadRawResourceStyle(this, R.raw.mapstyle_night_hms);
        hMap.setMapStyle(styleOptions);
    }


    public void getLocation(){
        //create a fusedLocationProviderClient
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        //create a settingsClient
        settingsClient = LocationServices.getSettingsClient(this);
        mLocationRequest = new LocationRequest();
        // set the interval for location updates, in milliseconds.
        mLocationRequest.setInterval(10000);
        // set the priority of the request
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult != null) {

                    mLocation = locationResult.getLastLocation();
                    LatLng mLocationLatLng =  new LatLng(mLocation.getLatitude(),mLocation.getLongitude());

                    /*
                    hMap.addMarker(new MarkerOptions()
                            .position(mLocationLatLng)
                            .title("You are here lat: "+ mLocation.getLatitude() + " long: " + mLocation.getLongitude())
                    );
                    */


                    try {

                        DirectionsService("driving",mLocationLatLng,destination);
                        DirectionsService("walking",mLocationLatLng,destination);
                        DirectionsService("bicycling",mLocationLatLng,destination);

                    } catch (UnsupportedEncodingException e) {

                        Log.e("error",e.getMessage());

                    }


                }
            }

            @Override
            public void onLocationAvailability(LocationAvailability locationAvailability) {
                if (locationAvailability != null) {
                    boolean flag = locationAvailability.isLocationAvailable();
                    Log.i(TAG, "onLocationAvailability isLocationAvailable:" + flag);
                }
            }
        };

        requestLocationUpdatesWithCallback();

    }




    public void DirectionsService(String serviceName,LatLng originLatLng, LatLng destinationLatLng) throws UnsupportedEncodingException {

        String ROOT_URL = "https://mapapi.cloud.huawei.com/mapApi/v1/routeService/";

        String conection = "?key=";

        MediaType JSON = MediaType.parse("application/json; charset=utf-8");

        JSONObject json = new JSONObject();
        JSONObject origin = new JSONObject();
        JSONObject destination = new JSONObject();

        try {

            origin.put("lng", originLatLng.longitude);
            origin.put("lat", originLatLng.latitude);

            destination.put("lng", destinationLatLng.longitude);
            destination.put("lat", destinationLatLng.latitude);

            json.put("origin", origin);
            json.put("destination", destination);

        } catch (JSONException e) {

            Log.e("error", e.getMessage());

        }

        RequestBody body = RequestBody.create(JSON, String.valueOf(json));

        OkHttpClient client = new OkHttpClient();
        Request request =
                new Request.Builder().url(ROOT_URL + serviceName + conection + URLEncoder.encode("CgB6e3x9OgwV9lrubpb5P0yCfo4sh3XaTtuxdkmrXJfXsWEpX5a9x/ImEsf93/b4YLpIRyhe8bp9S7ngsZmAsFFw", "UTF-8"))
                        .post(body)
                        .build();

        if(serviceName == "driving"){

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e("DirectionsService", e.toString());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    setPolyLineFromResponse(response.body().string(),0);
                }
            });

        } else if(serviceName == "walking"){

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e("DirectionsService", e.toString());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    setPolyLineFromResponse(response.body().string(),1);
                }
            });

        }else if(serviceName == "bicycling"){

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e("DirectionsService", e.toString());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    setPolyLineFromResponse(response.body().string(),2);
                }
            });

        }


    }



    private void requestLocationUpdatesWithCallback() {
        try {
            LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
            builder.addLocationRequest(mLocationRequest);
            LocationSettingsRequest locationSettingsRequest = builder.build();
            // check devices settings before request location updates.
            settingsClient.checkLocationSettings(locationSettingsRequest)
                    .addOnSuccessListener(new OnSuccessListener<LocationSettingsResponse>() {
                        @Override
                        public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                            Log.i(TAG, "check location settings success");
                            //request location updates
                            fusedLocationProviderClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.getMainLooper()).addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {
                                    Log.i(TAG, "requestLocationUpdatesWithCallback onSuccess");
                                }
                            })
                                    .addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(Exception e) {
                                            Log.e(TAG,
                                                    "requestLocationUpdatesWithCallback onFailure:" + e.getMessage());
                                        }
                                    });
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(Exception e) {
                            Log.e(TAG, "checkLocationSetting onFailure:" + e.getMessage());
                            int statusCode = ((ApiException) e).getStatusCode();
                            switch (statusCode) {
                                case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                                    try {
                                        ResolvableApiException rae = (ResolvableApiException) e;
                                        rae.startResolutionForResult(MainActivity.this, 0);
                                    } catch (IntentSender.SendIntentException sie) {
                                        Log.e(TAG, "PendingIntent unable to execute request.");
                                    }
                                    break;
                            }
                        }
                    });
        } catch (Exception e) {
            Log.e(TAG, "requestLocationUpdatesWithCallback exception:" + e.getMessage());
        }
    }

    private void removeLocationUpdatesWithCallback() {
        try {
            fusedLocationProviderClient.removeLocationUpdates(mLocationCallback)
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            Log.i(TAG, "removeLocationUpdatesWithCallback onSuccess");
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(Exception e) {
                            Log.e(TAG, "removeLocationUpdatesWithCallback onFailure:" + e.getMessage());
                        }
                    });
        } catch (Exception e) {
            Log.e(TAG, "removeLocationUpdatesWithCallback exception:" + e.getMessage());
        }
    }



    @Override
    protected void onStart() {
        super.onStart();
        mMapView.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mMapView.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mMapView.onDestroy();
    }

    @Override
    protected void onPause() {
        mMapView.onPause();
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mMapView.onResume();
    }

    private static boolean hasPermissions(Context context, String... permissions) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }

    public int getStartingIndex(int startIndex, String response){

        String polyline = "polyline";
        char[] poly = new char[8];
        String polyStr = "";

        for(int i = startIndex; i<response.length();i++){

            if(i+polyline.length() < response.length()){
                response.getChars(i,i+polyline.length(),poly,0);
                polyStr = String.valueOf(poly);

                if(polyStr.contains(polyline)){
                    //System.out.println(polyStr);
                    return i;

                }
            }
        }
        return 0;
    }

    public int getEndingIndex(int startingIndex,String response){

        for(int i = startingIndex; i<response.length();i++){
            if(response.charAt(i) == ']'){

                return i;

            }
        }
        return 0;
    }

    public void  setPolyLineFromResponse(String response, int roadIndex) {

        ArrayList<Integer> startingIndexList = new ArrayList<Integer>();
        ArrayList<Integer> endingIndexList = new ArrayList<Integer>();

        startingIndexList.add(getStartingIndex(0, response));
        endingIndexList.add(getEndingIndex(startingIndexList.get(0), response));


        while (getStartingIndex(endingIndexList.get(endingIndexList.size() - 1), response) != 0) {
            startingIndexList.add(getStartingIndex(endingIndexList.get(endingIndexList.size() - 1), response));
            endingIndexList.add(getEndingIndex(startingIndexList.get(startingIndexList.size() - 1), response));
        }

        ArrayList<Float> latArray = new ArrayList<Float>();
        ArrayList<Float> longArray = new ArrayList<Float>();
        String longStr = "";
        String latStr = "";
        for (int k = 0; k < startingIndexList.size(); k++) {

            for (int i = startingIndexList.get(k); i < endingIndexList.get(k); i++) {
                //System.out.print(string.charAt(i));

                if (response.charAt(i) == 'l' && response.charAt(i + 1) == 'n' && response.charAt(i + 2) == 'g') {
                    for (int j = i + 3; response.charAt(j) != ','; j++) {
                        if (response.charAt(j) != ':' && response.charAt(j) != '"' && response.charAt(j) != ' ') {
                            longStr = longStr + response.charAt(j);
                        }

                    }
                    //System.out.println(longStr);
                    longArray.add(Float.valueOf(longStr));
                    longStr = "";

                }
                if (response.charAt(i) == 'l' && response.charAt(i + 1) == 'a' && response.charAt(i + 2) == 't') {
                    for (int j = i + 3; response.charAt(j) != '}'; j++) {
                        if (response.charAt(j) != ':' && response.charAt(j) != '"' && response.charAt(j) != ' ') {
                            latStr = latStr + response.charAt(j);
                        }

                    }
                    //System.out.println(latStr);
                    latArray.add(Float.valueOf(latStr));
                    latStr = "";

                }

            }
        }

        List<LatLng> points = new ArrayList<LatLng>();
        for(int i = 0; i<latArray.size(); i++){
            points.add(new LatLng(latArray.get(i),longArray.get(i)));
        }

        Roads.get(roadIndex).points = points;

    }

    public void addPolyLine(int roadIndex){


        if(Roads.get(roadIndex).points != null){

            Roads.get(roadIndex).polyline = hMap.addPolyline(new PolylineOptions()
                    // Set the coordinates of a polyline.
                    // Set the color of a polyline.
                    .color(Roads.get(roadIndex).color)
                    // Set the polyline width.
                    .width(3));

            Roads.get(roadIndex).polyline.setPoints(Roads.get(roadIndex).points);

        }

    }

    public void DrivingButtonClick(View view){

        //Toast.makeText(getApplicationContext(),"Driving Clicked",Toast.LENGTH_LONG).show();
        clearPolyLines();
        addPolyLine(0);
    }

    public void WalkingButtonClick(View view){
        //Toast.makeText(getApplicationContext(),"Walking Clicked",Toast.LENGTH_LONG).show();
        clearPolyLines();
        addPolyLine(1);
    }

    public void BicyclingButtonClick(View view){
        //Toast.makeText(getApplicationContext(),"Bicycling Clicked",Toast.LENGTH_LONG).show();
        clearPolyLines();
        addPolyLine(2);
    }

    public void clearPolyLines(){
        if(Roads.get(0).polyline != null){
            Roads.get(0).polyline.remove();
        }
        if(Roads.get(1).polyline != null){
            Roads.get(1).polyline.remove();
        }
        if(Roads.get(2).polyline != null){
            Roads.get(2).polyline.remove();
        }
    }

}

