package com.example.frontend_bookingcare.api;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface DoctorChatApiService {

    @GET("api/v1/doctors/messages/threads")
    Call<ApiEnvelope> listThreads(@Header("Authorization") String authorization);

    @GET("api/v1/doctors/messages/threads/{threadKey}/messages")
    Call<ApiEnvelope> listMessages(@Header("Authorization") String authorization,
                                   @Path(value = "threadKey", encoded = true) String threadKey);

    @POST("api/v1/doctors/messages/threads/{threadKey}/messages")
    Call<ApiEnvelope> sendMessage(@Header("Authorization") String authorization,
                                  @Path(value = "threadKey", encoded = true) String threadKey,
                                  @Body SendChatMessageRequest body);

    @POST("api/v1/doctors/messages/threads/{threadKey}/read")
    Call<ApiEnvelope> markRead(@Header("Authorization") String authorization,
                               @Path(value = "threadKey", encoded = true) String threadKey);
}
