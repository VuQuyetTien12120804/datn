package com.example.frontend_bookingcare.api;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Query;

/**
 * 3 endpoint phục vụ luồng đặt lịch khám:
 *  - /working-dates : lấy các ngày bác sĩ có slot trống (từ hôm nay trở đi).
 *  - /slots         : lấy các khung giờ của 1 ngày cụ thể.
 *  - /book          : gửi request đặt lịch → backend lưu appointment, giảm slot.
 */
public interface BookingApiService {

    @GET("api/v1/booking/working-dates")
    Call<ApiEnvelope> getWorkingDates(@Query("doctorId") int doctorId);

    @GET("api/v1/booking/slots")
    Call<ApiEnvelope> getSlots(@Query("doctorId") int doctorId,
                               @Query("slotDate") String slotDate);

    @POST("api/v1/booking/book")
    Call<ApiEnvelope> book(@Header("Authorization") String authorization, @Body BookingRequest request);
}
