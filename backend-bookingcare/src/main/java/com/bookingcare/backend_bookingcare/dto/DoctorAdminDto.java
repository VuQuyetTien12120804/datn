package com.bookingcare.backend_bookingcare.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class DoctorAdminDto {

    @JsonProperty("doctorId")
    private Integer doctorId;

    @JsonProperty("accountId")
    private Integer accountId;

    @JsonProperty("fullName")
    private String fullName;

    private String gender;
    private String dob;
    private String phone;
    private String email;

    @JsonProperty("licenseNo")
    private String licenseNo;

    private String bio;

    @JsonProperty("avatarUrl")
    private String avatarUrl;

    private Double rating;

    @JsonProperty("visitsCount")
    private Integer visitsCount;

    @JsonProperty("roomLocation")
    private String roomLocation;

    @JsonProperty("scheduleText")
    private String scheduleText;

    private List<String> education;
    private List<String> certificates;

    @JsonProperty("createdAt")
    private String createdAt;

    @JsonProperty("updatedAt")
    private String updatedAt;

    @JsonProperty("specialtyIds")
    private List<Integer> specialtyIds;
}
