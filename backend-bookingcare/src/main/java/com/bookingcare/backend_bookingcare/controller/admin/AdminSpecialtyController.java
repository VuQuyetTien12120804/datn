package com.bookingcare.backend_bookingcare.controller.admin;

import com.bookingcare.backend_bookingcare.common.ApiEnvelope;
import com.bookingcare.backend_bookingcare.dto.SpecialtyDto;
import com.bookingcare.backend_bookingcare.dto.UpsertSpecialtyRequest;
import com.bookingcare.backend_bookingcare.service.AdminSpecialtyService;
import jakarta.validation.Valid;
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

@RestController
@RequestMapping("/api/v1/admin/specialties")
@RequiredArgsConstructor
public class AdminSpecialtyController {

    private final AdminSpecialtyService adminSpecialtyService;

    @GetMapping
    public ApiEnvelope<List<SpecialtyDto>> list() {
        return ApiEnvelope.ok(adminSpecialtyService.list());
    }

    @PostMapping
    public ApiEnvelope<SpecialtyDto> create(@Valid @RequestBody UpsertSpecialtyRequest request) {
        return ApiEnvelope.ok(adminSpecialtyService.create(request));
    }

    @PutMapping("/{id}")
    public ApiEnvelope<SpecialtyDto> update(@PathVariable int id, @Valid @RequestBody UpsertSpecialtyRequest request) {
        return ApiEnvelope.ok(adminSpecialtyService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiEnvelope<Void> delete(@PathVariable int id) {
        adminSpecialtyService.delete(id);
        return ApiEnvelope.ok(null);
    }
}
