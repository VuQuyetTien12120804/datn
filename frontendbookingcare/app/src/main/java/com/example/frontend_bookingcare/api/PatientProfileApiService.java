package com.example.frontend_bookingcare.api;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.PUT;

public interface PatientProfileApiService {
    @GET("api/v1/patient/profile")
    Call<ApiEnvelope> me(@Header("Authorization") String authorization);

    @PUT("api/v1/patient/profile")
    Call<ApiEnvelope> updateMe(@Header("Authorization") String authorization, @Body UpdatePatientProfileRequest body);
}

