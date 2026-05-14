package com.example.frontend_bookingcare.api;

public class UpdatePatientProfileRequest {
    public String phone;
    public String dob; // dd/MM/yyyy
    public String gender; // male/female/other
    public String address;

    public UpdatePatientProfileRequest() {
    }

    public UpdatePatientProfileRequest(String phone, String dob, String gender, String address) {
        this.phone = phone;
        this.dob = dob;
        this.gender = gender;
        this.address = address;
    }
}

