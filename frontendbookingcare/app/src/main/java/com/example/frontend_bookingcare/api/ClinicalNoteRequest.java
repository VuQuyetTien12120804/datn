package com.example.frontend_bookingcare.api;

import com.google.gson.annotations.SerializedName;

public class ClinicalNoteRequest {

    @SerializedName("note")
    public String note;

    public ClinicalNoteRequest(String note) {
        this.note = note;
    }
}
