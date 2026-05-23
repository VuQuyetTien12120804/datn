package com.bookingcare.backend_bookingcare.service;

import com.bookingcare.backend_bookingcare.entity.Account;
import com.bookingcare.backend_bookingcare.entity.ClinicService;
import com.bookingcare.backend_bookingcare.entity.Patient;
import com.bookingcare.backend_bookingcare.entity.Room;
import com.bookingcare.backend_bookingcare.entity.Role;
import com.bookingcare.backend_bookingcare.repository.AccountRepository;
import com.bookingcare.backend_bookingcare.repository.ClinicServiceRepository;
import com.bookingcare.backend_bookingcare.repository.PatientRepository;
import com.bookingcare.backend_bookingcare.repository.RoleRepository;
import com.bookingcare.backend_bookingcare.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminLookupService {

    private final PatientRepository patientRepository;
    private final RoomRepository roomRepository;
    private final ClinicServiceRepository clinicServiceRepository;
    private final AccountRepository accountRepository;
    private final RoleRepository roleRepository;

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listPatients() {
        return patientRepository.findAll().stream().map(this::patientMap).toList();
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listRooms() {
        return roomRepository.findAll().stream().map(this::roomMap).toList();
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listServices(Integer specialtyId) {
        List<ClinicService> list = specialtyId != null
                ? clinicServiceRepository.findBySpecialtyId(specialtyId)
                : clinicServiceRepository.findAll();
        return list.stream().map(this::serviceMap).toList();
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listAccounts() {
        return accountRepository.findAll().stream().map(this::accountMap).toList();
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listRoles() {
        return roleRepository.findAll().stream()
                .filter(r -> !Boolean.TRUE.equals(r.getDeleted()))
                .map(this::roleMap)
                .toList();
    }

    public Map<String, Object> patientMap(Patient p) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", p.getId());
        m.put("accountId", p.getAccountId());
        m.put("fullName", p.getFullName());
        m.put("dob", p.getDob() != null ? p.getDob().toString() : null);
        m.put("gender", p.getGender());
        m.put("phone", p.getPhone());
        m.put("email", p.getEmail());
        m.put("address", p.getAddress());
        m.put("insuranceNo", p.getInsuranceNo());
        m.put("emergencyContactName", p.getEmergencyContactName());
        m.put("emergencyContactPhone", p.getEmergencyContactPhone());
        m.put("createdAt", p.getCreatedAt() != null ? p.getCreatedAt().toString() : null);
        m.put("updatedAt", p.getUpdatedAt() != null ? p.getUpdatedAt().toString() : null);
        return m;
    }

    public Map<String, Object> roomMap(Room r) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", r.getId());
        m.put("code", r.getCode());
        m.put("name", r.getName());
        m.put("floor", r.getFloor());
        m.put("note", r.getNote());
        m.put("createdAt", r.getCreatedAt() != null ? r.getCreatedAt().toString() : null);
        m.put("updatedAt", r.getUpdatedAt() != null ? r.getUpdatedAt().toString() : null);
        return m;
    }

    public Map<String, Object> serviceMap(ClinicService s) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", s.getId());
        m.put("specialtyId", s.getSpecialtyId());
        m.put("code", s.getCode());
        m.put("name", s.getName());
        m.put("description", s.getDescription());
        m.put("durationMinutes", s.getDurationMinutes());
        m.put("priceCents", s.getPriceCents());
        m.put("isActive", s.getActive());
        m.put("createdAt", s.getCreatedAt() != null ? s.getCreatedAt().toString() : null);
        m.put("updatedAt", s.getUpdatedAt() != null ? s.getUpdatedAt().toString() : null);
        return m;
    }

    public Map<String, Object> accountMap(Account a) {
        Map<String, Object> m = new HashMap<>();
        m.put("userId", a.getId());
        m.put("email", a.getEmail());
        m.put("phone", a.getPhone());
        m.put("fullName", a.getFullName());
        m.put("status", a.getStatus());
        m.put("isEmailVerified", a.getEmailVerified());
        m.put("lastLoginAt", a.getLastLoginAt() != null ? a.getLastLoginAt().toString() : null);
        m.put("createdAt", a.getCreatedAt() != null ? a.getCreatedAt().toString() : null);
        m.put("updatedAt", a.getUpdatedAt() != null ? a.getUpdatedAt().toString() : null);
        if (a.getRole() != null) {
            m.put("role", roleMap(a.getRole()));
        }
        return m;
    }

    private Map<String, Object> roleMap(Role r) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", r.getId());
        m.put("code", r.getCode());
        m.put("name", r.getName());
        m.put("description", r.getDescription());
        return m;
    }
}
