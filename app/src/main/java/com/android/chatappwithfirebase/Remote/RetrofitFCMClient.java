package com.android.chatappwithfirebase.Remote;

import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

//todo 4 send notification (next ChatActivity)
public class RetrofitFCMClient {

    private static Retrofit instance;
    public static Retrofit getInstance(){
        return instance == null ?
                new Retrofit.Builder()
                .baseUrl("https://fcm.googleapis.com/")
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .addConverterFactory(GsonConverterFactory.create())
                .build() : instance;
    }
}
