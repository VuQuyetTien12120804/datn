package com.example.frontend_bookingcare.api;

import com.google.gson.JsonElement;

public class ApiEnvelope {
    public boolean success;
    public int code;
    public String message;
    public JsonElement data;
}
