package com.bookingcare.backend_bookingcare.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SpecialtyDto {

    @JsonProperty("specialtyId")
    private Integer specialtyId;

    private String code;
    private String name;
    private String description;

    @JsonProperty("createdAt")
    private String createdAt;
}
