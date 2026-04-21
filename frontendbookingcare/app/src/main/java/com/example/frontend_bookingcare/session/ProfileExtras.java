package com.example.frontend_bookingcare.session;

public class ProfileExtras {
    public String phone = "";
    public String dob = "";
    public String gender = "";
    public String address = "";

    public ProfileExtras() {
    }

    public ProfileExtras(String phone, String dob, String gender, String address) {
        this.phone = phone != null ? phone : "";
        this.dob = dob != null ? dob : "";
        this.gender = gender != null ? gender : "";
        this.address = address != null ? address : "";
    }
}
