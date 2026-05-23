package com.bookingcare.backend_bookingcare.controller.admin;

import com.bookingcare.backend_bookingcare.common.ApiEnvelope;
import com.bookingcare.backend_bookingcare.service.AdminLookupService;
import com.bookingcare.backend_bookingcare.service.AdminManagementService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/accounts")
@RequiredArgsConstructor
public class AdminAccountController {

    private final AdminLookupService adminLookupService;
    private final AdminManagementService adminManagementService;

    @GetMapping
    public ApiEnvelope<List<Map<String, Object>>> list() {
        return ApiEnvelope.ok(adminLookupService.listAccounts());
    }

    @GetMapping("/roles")
    public ApiEnvelope<List<Map<String, Object>>> roles() {
        return ApiEnvelope.ok(adminLookupService.listRoles());
    }

    @PostMapping
    public ApiEnvelope<Map<String, Object>> create(@RequestBody Map<String, Object> body) {
        return ApiEnvelope.ok(adminManagementService.createAccount(body));
    }

    @PutMapping("/{id}")
    public ApiEnvelope<Map<String, Object>> update(@PathVariable int id, @RequestBody Map<String, Object> body) {
        return ApiEnvelope.ok(adminManagementService.updateAccount(id, body));
    }

    @PatchMapping("/{id}/block")
    public ApiEnvelope<Map<String, Object>> block(@PathVariable int id) {
        return ApiEnvelope.ok(adminManagementService.blockAccount(id));
    }

    @PatchMapping("/{id}/unblock")
    public ApiEnvelope<Map<String, Object>> unblock(@PathVariable int id) {
        return ApiEnvelope.ok(adminManagementService.unblockAccount(id));
    }
}
