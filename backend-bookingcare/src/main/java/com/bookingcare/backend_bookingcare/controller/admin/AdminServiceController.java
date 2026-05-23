package com.bookingcare.backend_bookingcare.controller.admin;

import com.bookingcare.backend_bookingcare.common.ApiEnvelope;
import com.bookingcare.backend_bookingcare.service.AdminLookupService;
import com.bookingcare.backend_bookingcare.service.AdminManagementService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/services")
@RequiredArgsConstructor
public class AdminServiceController {

    private final AdminLookupService adminLookupService;
    private final AdminManagementService adminManagementService;

    @GetMapping
    public ApiEnvelope<List<Map<String, Object>>> list(@RequestParam(required = false) Integer specialtyId) {
        return ApiEnvelope.ok(adminLookupService.listServices(specialtyId));
    }

    @PostMapping
    public ApiEnvelope<Map<String, Object>> create(@RequestBody Map<String, Object> body) {
        return ApiEnvelope.ok(adminManagementService.createService(body));
    }

    @PutMapping("/{id}")
    public ApiEnvelope<Map<String, Object>> update(@PathVariable int id, @RequestBody Map<String, Object> body) {
        return ApiEnvelope.ok(adminManagementService.updateService(id, body));
    }

    @DeleteMapping("/{id}")
    public ApiEnvelope<Void> delete(@PathVariable int id) {
        adminManagementService.deleteService(id);
        return ApiEnvelope.ok(null);
    }
}
