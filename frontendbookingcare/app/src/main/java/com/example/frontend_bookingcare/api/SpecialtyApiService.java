package com.example.frontend_bookingcare.api;

import retrofit2.Call;
import retrofit2.http.GET;

public interface SpecialtyApiService {

    @GET("api/v1/specialties")
    Call<ApiEnvelope> getAllSpecialties();
}
