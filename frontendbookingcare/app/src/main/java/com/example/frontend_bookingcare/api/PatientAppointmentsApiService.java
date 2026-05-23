package com.example.frontend_bookingcare.api;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Body;
import retrofit2.http.Path;
import retrofit2.http.Query;

/** API cho bệnh nhân xem lịch đã đặt (JWT). */
public interface PatientAppointmentsApiService {
    @GET("api/v1/patient/appointments")
    Call<ApiEnvelope> myAppointments(@Header("Authorization") String authorization,
                                     @Query("group") String group);

    @GET("api/v1/patient/appointments/{appointmentId}")
    Call<ApiEnvelope> appointmentDetail(@Header("Authorization") String authorization,
                                        @Path("appointmentId") int appointmentId);

    @POST("api/v1/patient/appointments/{appointmentId}/cancel")
    Call<ApiEnvelope> cancelAppointment(@Header("Authorization") String authorization,
                                        @Path("appointmentId") int appointmentId,
                                        @Body CancelAppointmentRequest body);

    @POST("api/v1/patient/appointments/{appointmentId}/check-in")
    Call<ApiEnvelope> checkInAppointment(@Header("Authorization") String authorization,
                                         @Path("appointmentId") int appointmentId);
}

