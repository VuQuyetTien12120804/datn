package com.bookingcare.backend_bookingcare.controller;

import com.bookingcare.backend_bookingcare.common.ApiEnvelope;
import com.bookingcare.backend_bookingcare.service.DoctorPublicService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/doctors")
@RequiredArgsConstructor
public class DoctorController {

    private final DoctorPublicService doctorPublicService;

    @GetMapping
    public ApiEnvelope<List<Map<String, Object>>> list() {
        return ApiEnvelope.ok(doctorPublicService.listAll());
    }

    @GetMapping("/specialty/{specialtyId}")
    public ApiEnvelope<List<Map<String, Object>>> bySpecialty(@PathVariable int specialtyId) {
        return ApiEnvelope.ok(doctorPublicService.listBySpecialty(specialtyId));
    }
}
