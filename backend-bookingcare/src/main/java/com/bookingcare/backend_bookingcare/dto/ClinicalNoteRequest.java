package com.bookingcare.backend_bookingcare.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ClinicalNoteRequest {

    @JsonProperty("note")
    private String note;
}
