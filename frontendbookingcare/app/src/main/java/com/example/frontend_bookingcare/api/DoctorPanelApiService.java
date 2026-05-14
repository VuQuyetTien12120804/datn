package com.example.frontend_bookingcare.api;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.PATCH;
import retrofit2.http.Path;
import retrofit2.http.Query;

/** API dành cho tài khoản bác sĩ (JWT). */
public interface DoctorPanelApiService {

    @GET("api/v1/doctors/appointment-status")
    Call<ApiEnvelope> getAppointmentsByStatus(@Header("Authorization") String authorization,
                                              @Query("status") String status);

    @GET("api/v1/doctors/appointment-today")
    Call<ApiEnvelope> getTodayAppointments(@Header("Authorization") String authorization);

    /**
     * Chi tiết cuộc hẹn kèm {@code patientDob} / {@code patientGender} (đầy đủ như admin detail).
     */
    @GET("api/v1/doctors/appointment/{appointmentId}/detail")
    Call<ApiEnvelope> getAppointmentDetail(@Header("Authorization") String authorization,
                                            @Path("appointmentId") int appointmentId);

    @PATCH("api/v1/doctors/appointment/{appointmentId}/confirm")
    Call<ApiEnvelope> confirmAppointment(@Header("Authorization") String authorization,
                                         @Path("appointmentId") int appointmentId);

    @PATCH("api/v1/doctors/appointment/{appointmentId}/cancel")
    Call<ApiEnvelope> cancelAppointment(@Header("Authorization") String authorization,
                                        @Path("appointmentId") int appointmentId);
}
