package com.bookingcare.backend_bookingcare.controller;

import com.bookingcare.backend_bookingcare.common.ApiEnvelope;
import com.bookingcare.backend_bookingcare.service.LegalService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/legal-documents")
@RequiredArgsConstructor
public class LegalController {

    private final LegalService legalService;

    @GetMapping("/{code}")
    public ApiEnvelope<Map<String, Object>> get(@PathVariable String code) {
        return ApiEnvelope.ok(legalService.getByCode(code));
    }
}
