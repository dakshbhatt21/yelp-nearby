package com.example.yelpnearbyapp.Interface;

import java.util.Map;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.QueryMap;

public interface ApiInterface {

    @GET("search")
    Call<ResponseBody> getRestaurants(@Header("Authorization") String key, @QueryMap Map<String, String> params);

}
