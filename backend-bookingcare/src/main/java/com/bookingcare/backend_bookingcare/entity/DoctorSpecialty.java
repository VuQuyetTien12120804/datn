package com.bookingcare.backend_bookingcare.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "doctor_specialties", schema = "dbo")
@IdClass(DoctorSpecialtyId.class)
public class DoctorSpecialty {

    @Id
    @Column(name = "doctor_id")
    private Integer doctorId;

    @Id
    @Column(name = "specialty_id")
    private Integer specialtyId;
}
