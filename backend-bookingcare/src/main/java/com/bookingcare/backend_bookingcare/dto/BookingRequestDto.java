package com.bookingcare.backend_bookingcare.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BookingRequestDto {

    @JsonProperty("doctorId")
    private Integer doctorId;

    @JsonProperty("slotId")
    private Integer slotId;

    @JsonProperty("appointmentDate")
    private String appointmentDate;

    @JsonProperty("fullName")
    private String fullName;

    private String email;

    @JsonProperty("phoneNumber")
    private String phoneNumber;

    private String dob;

    private String address;

    private String gender;

    private String notes;
}
