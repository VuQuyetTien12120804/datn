package com.bookingcare.backend_bookingcare.controller.admin;

import com.bookingcare.backend_bookingcare.common.ApiEnvelope;
import com.bookingcare.backend_bookingcare.dto.DoctorAdminDto;
import com.bookingcare.backend_bookingcare.service.AdminDoctorService;
import com.bookingcare.backend_bookingcare.service.AdminManagementService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/doctors")
@RequiredArgsConstructor
public class AdminDoctorController {

    private final AdminDoctorService adminDoctorService;
    private final AdminManagementService adminManagementService;

    @GetMapping
    public ApiEnvelope<List<DoctorAdminDto>> list() {
        return ApiEnvelope.ok(adminDoctorService.list());
    }

    @PostMapping
    public ApiEnvelope<DoctorAdminDto> create(@RequestBody Map<String, Object> body) {
        return ApiEnvelope.ok(adminManagementService.createDoctor(body));
    }

    @PutMapping("/{id}")
    public ApiEnvelope<DoctorAdminDto> update(@PathVariable int id, @RequestBody Map<String, Object> body) {
        return ApiEnvelope.ok(adminManagementService.updateDoctor(id, body));
    }

    @DeleteMapping("/{id}")
    public ApiEnvelope<Void> delete(@PathVariable int id) {
        adminManagementService.deleteDoctor(id);
        return ApiEnvelope.ok(null);
    }
}
