package com.example.frontend_bookingcare.api;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface DoctorApiService {

    @GET("api/v1/doctors")
    Call<ApiEnvelope> getAllDoctors();

    @GET("api/v1/doctors/specialty/{specialtyId}")
    Call<ApiEnvelope> getDoctorsBySpecialty(@Path("specialtyId") int specialtyId);
}
