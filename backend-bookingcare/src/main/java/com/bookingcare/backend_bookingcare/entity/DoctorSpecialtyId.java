package com.bookingcare.backend_bookingcare.entity;

import lombok.EqualsAndHashCode;

import java.io.Serializable;

@EqualsAndHashCode
public class DoctorSpecialtyId implements Serializable {
    private Integer doctorId;
    private Integer specialtyId;
}
