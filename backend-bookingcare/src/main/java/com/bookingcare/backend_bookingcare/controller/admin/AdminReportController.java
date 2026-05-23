package com.bookingcare.backend_bookingcare.controller.admin;

import com.bookingcare.backend_bookingcare.common.ApiEnvelope;
import com.bookingcare.backend_bookingcare.service.AdminReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/reports")
@RequiredArgsConstructor
public class AdminReportController {

    private final AdminReportService adminReportService;

    @GetMapping("/appointments/summary")
    public ApiEnvelope<Map<String, Object>> appointmentSummary(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to
    ) {
        return ApiEnvelope.ok(adminReportService.appointmentSummary(from, to));
    }

    @GetMapping("/appointments/timeseries")
    public ApiEnvelope<List<Map<String, Object>>> timeseries(
            @RequestParam(defaultValue = "day") String granularity,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to
    ) {
        return ApiEnvelope.ok(adminReportService.timeseries(from, to, granularity));
    }

    @GetMapping("/appointments/top-doctors")
    public ApiEnvelope<List<Map<String, Object>>> topDoctors(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(defaultValue = "10") int limit
    ) {
        return ApiEnvelope.ok(adminReportService.topDoctors(from, to, limit));
    }

    @GetMapping("/appointments/top-specialties")
    public ApiEnvelope<List<Map<String, Object>>> topSpecialties(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(defaultValue = "10") int limit
    ) {
        return ApiEnvelope.ok(adminReportService.topSpecialties(from, to, limit));
    }
}
