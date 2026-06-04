package com.example.frontend_bookingcare.api;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Path;

/** Retrofit interface cho 3 endpoint chatbot. */
public interface ChatbotApiService {

    @POST("api/v1/chatbot/sessions")
    Call<ApiEnvelope> openSession(@Header("Authorization") String authorization);

    @GET("api/v1/chatbot/sessions/{sessionId}/messages")
    Call<ApiEnvelope> listMessages(@Header("Authorization") String authorization,
                                   @Path("sessionId") long sessionId);

    @POST("api/v1/chatbot/sessions/{sessionId}/messages")
    Call<ApiEnvelope> sendMessage(@Header("Authorization") String authorization,
                                  @Path("sessionId") long sessionId,
                                  @Body ChatbotSendBody body);
}
