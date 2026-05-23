package com.bookingcare.backend_bookingcare.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpsertSpecialtyRequest {

    private String code;

    @NotBlank
    private String name;

    private String description;
}
