package com.bookingcare.backend_bookingcare.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Getter
@Setter
@Entity
@Table(name = "doctors", schema = "dbo")
public class Doctor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "account_id")
    private Integer accountId;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    private String gender;

    private LocalDate dob;

    private String phone;

    private String email;

    @Column(name = "license_no")
    private String licenseNo;

    private String bio;

    @Column(name = "avatar_url")
    private String avatarUrl;

    private BigDecimal rating;

    @Column(name = "visits_count")
    private Integer visitsCount;

    @Column(name = "room_location")
    private String roomLocation;

    @Column(name = "schedule_text")
    private String scheduleText;

    @Column(name = "education_json")
    private String educationJson;

    @Column(name = "certificates_json")
    private String certificatesJson;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}
