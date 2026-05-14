package com.example.frontend_bookingcare.api;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface LegalApiService {

    @GET("api/v1/legal-documents/{code}")
    Call<ApiEnvelope> getByCode(@Path("code") String code);
}
