package com.bookingcare.backend_bookingcare.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RefreshRequest {

    @NotBlank
    @JsonProperty("refreshToken")
    private String refreshToken;
}
