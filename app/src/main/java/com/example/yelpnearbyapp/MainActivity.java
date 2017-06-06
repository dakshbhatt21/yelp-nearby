package com.example.yelpnearbyapp;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.yelpnearbyapp.Interface.ApiInterface;
import com.example.yelpnearbyapp.Model.Restaurant;
import com.example.yelpnearbyapp.Service.ApiClient;
import com.example.yelpnearbyapp.Utility.Constants;
import com.example.yelpnearbyapp.Utility.HelperFunctions;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, OnMapReadyCallback, LocationListener, GoogleMap.OnInfoWindowClickListener {

    private GoogleMap mMap;
    private ProgressDialog progressDialog;
    private Button btnSearch;
    private SupportMapFragment mapFragment;

    private Map<String, String> hmRestaurantParams;
    private ArrayList<Restaurant> alRestaurant;

    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;

    private Context c;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        c = MainActivity.this;

        btnSearch = (Button) findViewById(R.id.search_btn);
        btnSearch.setEnabled(false);

        mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);

        btnSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                callApi();
            }
        });
    }

    public void callApi() {
        ApiInterface getRestaurantService = ApiClient.getRestaurantApi().create(ApiInterface.class);
        Call<ResponseBody> callSearch = getRestaurantService.getRestaurants("Bearer " + Constants.key, hmRestaurantParams);

        progressDialog = new ProgressDialog(MainActivity.this);
        progressDialog.setIndeterminate(true);
        progressDialog.setMessage("Loading...");
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.show();

        callSearch.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (200 == response.code()) {
                    try {
                        progressDialog.dismiss();
                        alRestaurant = new ArrayList<>();
                        String res = response.body().string();

                        JSONObject jobj = new JSONObject(res);
                        JSONArray restaurantArray = jobj.getJSONArray("businesses");
                        for (int i = 0; i < restaurantArray.length(); i++) {

                            JSONObject coordinates = restaurantArray.getJSONObject(i).getJSONObject("coordinates");

                            JSONObject locationobj = restaurantArray.getJSONObject(i).getJSONObject("location");
                            JSONArray addressArray = locationobj.getJSONArray("display_address");
                            JSONArray categoriesArray = restaurantArray.getJSONObject(i).getJSONArray("categories");

                            Restaurant restaurant = new Restaurant();
                            if (!coordinates.isNull("latitude")) {
                                restaurant.setLatitude(coordinates.getDouble("latitude"));
                            }
                            if (!coordinates.isNull("longitude")) {
                                restaurant.setLongitude(coordinates.getDouble("longitude"));
                            }
                            restaurant.setName(restaurantArray.getJSONObject(i).getString("name"));
                            restaurant.setRating(restaurantArray.getJSONObject(i).getString("rating"));

                            restaurant.setCategory(categoriesArray.getJSONObject(0).getString("title"));

                            restaurant.setPhoneNumber(restaurantArray.getJSONObject(i).getString("display_phone"));
                            restaurant.setRestaurantImage(restaurantArray.getJSONObject(i).getString("image_url"));
                            restaurant.setReviewCount(restaurantArray.getJSONObject(i).getString("review_count"));

                            String strAddress = "";
                            for (int j = 0; j < addressArray.length(); j++) {
                                strAddress += addressArray.getString(j) + ", \n";
                            }

                            restaurant.setAddress(strAddress);
                            alRestaurant.add(restaurant);
                        }

                        for (int i = 0; i < alRestaurant.size(); i++) {
                            addMarkerCustom(alRestaurant.get(i).getLatitude(), alRestaurant.get(i).getLongitude(), i);
                        }

                        if (alRestaurant.size() != 0) {
                            mMap.setInfoWindowAdapter(new CustomInfoWindowAdapter(alRestaurant));
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else if (401 == response.code())

                {
                    try {
                        progressDialog.dismiss();
                        Log.e("test", "401 response: " + response.errorBody().string());
                    } catch (IOException je) {

                    }
                } else if (400 == response.code()) {
                    try {
                        progressDialog.dismiss();
                        Log.e("test", "400 response: " + response.errorBody().string());
                    } catch (IOException je) {

                    }
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                progressDialog.dismiss();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (checkLocationPermission()) {
            if (HelperFunctions.isLocationEnabled(c)) {
                mapFragment.getMapAsync(this);
                if (isNetworkAvailable(MainActivity.this)) {
                    btnSearch.setEnabled(true);

                    if (mGoogleApiClient == null) {
                        mGoogleApiClient = new GoogleApiClient.Builder(this)
                                .addConnectionCallbacks(this)
                                .addOnConnectionFailedListener(this)
                                .addApi(LocationServices.API)
                                .build();

                        mGoogleApiClient.connect();
                    } else {
                        callApi();
                    }

                } else {
                    Toast.makeText(c, "please check your connection", Toast.LENGTH_SHORT).show();
                }
            } else {
                AlertDialog.Builder builder = new AlertDialog.Builder(c);
                builder.setTitle("GPS Turned Off");
                builder.setMessage("Please turn on GPS to find the nearest Yelp restaurants. Click yes to open the GPS Location settings page.");
                builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialogInterface, int i) {
                        startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                    }
                });
                builder.setNegativeButton("No", null);
                builder.create().show();
            }
        }

    }

    @Override
    protected void onStart() {
        if (null != mGoogleApiClient) {
            mGoogleApiClient.connect();
        }
        super.onStart();
    }

    @Override
    protected void onStop() {
        if (null != mGoogleApiClient) {
            mGoogleApiClient.disconnect();
        }
        super.onStop();
    }


    public Marker addMarkerCustom(double latitude, double longitude, int pos) {
        return mMap.addMarker(new MarkerOptions()
                .position(new LatLng(latitude, longitude))
                .snippet(String.valueOf(pos))
                .anchor(0.5f, 0.5f));
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setOnInfoWindowClickListener(this);
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        try {
            mLocationRequest = LocationRequest.create();
            mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);

            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        } catch (SecurityException se) {
            se.printStackTrace();
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
    }

    @Override
    public void onLocationChanged(Location location) {
        mMap.clear();

        hmRestaurantParams = new HashMap<>();
        hmRestaurantParams.put("term", "restaurants");
        hmRestaurantParams.put("latitude", String.valueOf(location.getLatitude()));
        hmRestaurantParams.put("longitude", String.valueOf(location.getLongitude()));

        Bitmap bitmap = HelperFunctions.getBitmap(MainActivity.this, R.drawable.ic_my_location_black_24dp);
        LatLng latlng = new LatLng(Double.parseDouble(hmRestaurantParams.get("latitude")), Double.parseDouble(hmRestaurantParams.get("longitude")));
        mMap.addMarker(new MarkerOptions()
                .position(latlng)
                .snippet("-1")
                .icon(BitmapDescriptorFactory.fromBitmap(bitmap)));

        CameraPosition cameraPosition = new CameraPosition.Builder().target(latlng).zoom(10).build();

        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
    }

    @Override
    public void onInfoWindowClick(Marker marker) {
        int position = Integer.parseInt(marker.getSnippet());
        if (position != -1) {
            Intent i = new Intent(MainActivity.this, RestaurentDetailActivity.class);
            i.putExtra("title", alRestaurant.get(position).getName());
            i.putExtra("address", alRestaurant.get(position).getAddress());
            i.putExtra("rating", alRestaurant.get(position).getRating());
            i.putExtra("category", alRestaurant.get(position).getCategory());
            i.putExtra("phone", alRestaurant.get(position).getPhoneNumber());
            i.putExtra("resImg", alRestaurant.get(position).getRestaurantImage());
            i.putExtra("reviewCount", alRestaurant.get(position).getReviewCount());

            startActivity(i);
        }
    }

    private class CustomInfoWindowAdapter implements GoogleMap.InfoWindowAdapter {

        private final View resContentView;
        private ArrayList<Restaurant> arrayData;

        CustomInfoWindowAdapter(ArrayList<Restaurant> arrayData) {
            this.arrayData = arrayData;
            resContentView = getLayoutInflater().inflate(R.layout.marker_detail_layout, null);
        }

        @Override
        public View getInfoContents(Marker marker) {
            TextView txtResName = ((TextView) resContentView.findViewById(R.id.restaurant_name));
            TextView txtRating = ((TextView) resContentView.findViewById(R.id.rating_point));
            TextView txtAddress = ((TextView) resContentView.findViewById(R.id.restaurant_address));
            ImageView imgRating = ((ImageView) resContentView.findViewById(R.id.img_rating));

            if (Integer.parseInt(marker.getSnippet()) != -1) {
                txtAddress.setVisibility(View.VISIBLE);
                txtRating.setVisibility(View.VISIBLE);
                imgRating.setVisibility(View.VISIBLE);
                int position = Integer.parseInt(marker.getSnippet());

                txtResName.setText(arrayData.get(position).getName());
                txtRating.setText(" " + arrayData.get(position).getRating() + "/5");
                txtAddress.setText(arrayData.get(position).getAddress().replaceAll(", $", "").trim());
            } else {
                txtResName.setText("Your Location");
                txtAddress.setVisibility(View.GONE);
                txtRating.setVisibility(View.GONE);
                imgRating.setVisibility(View.GONE);
            }

            return resContentView;
        }

        @Override
        public View getInfoWindow(Marker marker) {
            return null;
        }
    }

    public static boolean isNetworkAvailable(Context c) {
        ConnectivityManager connectivityManager = (ConnectivityManager) c.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    public boolean checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                new AlertDialog.Builder(this)
                        .setTitle("Location Permission")
                        .setMessage("Please turn on location permission to find the nearest Yelp Restaurants.")
                        .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 99);
                            }
                        })
                        .create()
                        .show();
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 99);
            }
            return false;
        } else {
            return true;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 99: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    }
                } else {
                    Toast.makeText(c, "please turn on location to display nearest Yelp Restaurants.", Toast.LENGTH_SHORT).show();
                }
                return;
            }
        }
    }

}
