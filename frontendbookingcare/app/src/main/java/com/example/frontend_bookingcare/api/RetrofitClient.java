package com.example.frontend_bookingcare.api;

import com.example.frontend_bookingcare.BuildConfig;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public final class RetrofitClient {

    private static volatile Retrofit retrofit;
    private static volatile AuthApiService authApi;
    private static volatile DoctorApiService doctorApi;
    private static volatile SpecialtyApiService specialtyApi;
    private static final Gson GSON = new GsonBuilder().serializeNulls().create();

    private RetrofitClient() {
    }

    public static Gson gson() {
        return GSON;
    }

    private static Retrofit retrofit() {
        if (retrofit == null) {
            synchronized (RetrofitClient.class) {
                if (retrofit == null) {
                    OkHttpClient client = new OkHttpClient.Builder()
                            .connectTimeout(30, TimeUnit.SECONDS)
                            .readTimeout(30, TimeUnit.SECONDS)
                            .writeTimeout(30, TimeUnit.SECONDS)
                            .build();
                    retrofit = new Retrofit.Builder()
                            .baseUrl(BuildConfig.API_BASE_URL)
                            .client(client)
                            .addConverterFactory(GsonConverterFactory.create(GSON))
                            .build();
                }
            }
        }
        return retrofit;
    }

    public static AuthApiService authApi() {
        if (authApi == null) {
            synchronized (RetrofitClient.class) {
                if (authApi == null) {
                    authApi = retrofit().create(AuthApiService.class);
                }
            }
        }
        return authApi;
    }

    public static DoctorApiService doctorApi() {
        if (doctorApi == null) {
            synchronized (RetrofitClient.class) {
                if (doctorApi == null) {
                    doctorApi = retrofit().create(DoctorApiService.class);
                }
            }
        }
        return doctorApi;
    }

    public static SpecialtyApiService specialtyApi() {
        if (specialtyApi == null) {
            synchronized (RetrofitClient.class) {
                if (specialtyApi == null) {
                    specialtyApi = retrofit().create(SpecialtyApiService.class);
                }
            }
        }
        return specialtyApi;
    }
}
