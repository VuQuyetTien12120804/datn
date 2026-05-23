package com.bookingcare.backend_bookingcare.service;

import com.bookingcare.backend_bookingcare.common.ApiException;
import com.bookingcare.backend_bookingcare.dto.DoctorAdminDto;
import com.bookingcare.backend_bookingcare.entity.Account;
import com.bookingcare.backend_bookingcare.entity.AppointmentSlot;
import com.bookingcare.backend_bookingcare.entity.ClinicService;
import com.bookingcare.backend_bookingcare.entity.Doctor;
import com.bookingcare.backend_bookingcare.entity.DoctorSpecialty;
import com.bookingcare.backend_bookingcare.entity.Role;
import com.bookingcare.backend_bookingcare.entity.Patient;
import com.bookingcare.backend_bookingcare.entity.Room;
import com.bookingcare.backend_bookingcare.repository.AccountRepository;
import com.bookingcare.backend_bookingcare.repository.AppointmentSlotRepository;
import com.bookingcare.backend_bookingcare.repository.ClinicServiceRepository;
import com.bookingcare.backend_bookingcare.repository.DoctorRepository;
import com.bookingcare.backend_bookingcare.repository.DoctorSpecialtyRepository;
import com.bookingcare.backend_bookingcare.repository.PatientRepository;
import com.bookingcare.backend_bookingcare.repository.RoleRepository;
import com.bookingcare.backend_bookingcare.repository.RoomRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminManagementService {

    private final RoomRepository roomRepository;
    private final ClinicServiceRepository clinicServiceRepository;
    private final AccountRepository accountRepository;
    private final RoleRepository roleRepository;
    private final DoctorRepository doctorRepository;
    private final DoctorSpecialtyRepository doctorSpecialtyRepository;
    private final PatientRepository patientRepository;
    private final AppointmentSlotRepository slotRepository;
    private final AdminDoctorService adminDoctorService;
    private final AdminLookupService adminLookupService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // --- Rooms ---
    @Transactional
    public Map<String, Object> createRoom(Map<String, Object> body) {
        Room r = new Room();
        r.setCode(str(body.get("code")));
        r.setName(requireStr(body.get("name"), "name"));
        r.setFloor(str(body.get("floor")));
        r.setNote(str(body.get("note")));
        r.setCreatedAt(OffsetDateTime.now());
        r.setUpdatedAt(OffsetDateTime.now());
        return adminLookupService.roomMap(roomRepository.save(r));
    }

    @Transactional
    public Map<String, Object> updateRoom(int id, Map<String, Object> body) {
        Room r = roomRepository.findById(id).orElseThrow(() -> new ApiException(404, "Không tìm thấy phòng"));
        if (body.containsKey("code")) r.setCode(str(body.get("code")));
        if (body.containsKey("name")) r.setName(requireStr(body.get("name"), "name"));
        if (body.containsKey("floor")) r.setFloor(str(body.get("floor")));
        if (body.containsKey("note")) r.setNote(str(body.get("note")));
        r.setUpdatedAt(OffsetDateTime.now());
        return adminLookupService.roomMap(roomRepository.save(r));
    }

    @Transactional
    public void deleteRoom(int id) {
        roomRepository.deleteById(id);
    }

    // --- Services ---
    @Transactional
    public Map<String, Object> createService(Map<String, Object> body) {
        ClinicService s = mapService(new ClinicService(), body);
        s.setCreatedAt(OffsetDateTime.now());
        s.setUpdatedAt(OffsetDateTime.now());
        return adminLookupService.serviceMap(clinicServiceRepository.save(s));
    }

    @Transactional
    public Map<String, Object> updateService(int id, Map<String, Object> body) {
        ClinicService s = clinicServiceRepository.findById(id)
                .orElseThrow(() -> new ApiException(404, "Không tìm thấy dịch vụ"));
        mapService(s, body);
        s.setUpdatedAt(OffsetDateTime.now());
        return adminLookupService.serviceMap(clinicServiceRepository.save(s));
    }

    @Transactional
    public void deleteService(int id) {
        clinicServiceRepository.deleteById(id);
    }

    // --- Accounts ---
    @Transactional
    public Map<String, Object> createAccount(Map<String, Object> body) {
        int roleId = intVal(body.get("roleId"));
        Role role = roleRepository.findById(roleId).orElseThrow(() -> new ApiException(400, "Role không hợp lệ"));
        Account a = new Account();
        a.setRole(role);
        a.setEmail(str(body.get("email")));
        a.setPhone(str(body.get("phone")));
        a.setFullName(requireStr(body.get("fullName"), "fullName"));
        a.setPassword(str(body.get("password")) != null ? str(body.get("password")) : "changeme");
        a.setStatus(str(body.get("status")) != null ? str(body.get("status")) : "active");
        a.setEmailVerified(true);
        a.setCreatedAt(OffsetDateTime.now());
        a.setUpdatedAt(OffsetDateTime.now());
        Account saved = accountRepository.save(a);
        if ("patient".equalsIgnoreCase(role.getCode())
                && patientRepository.findByAccountId(saved.getId()).isEmpty()) {
            Patient patient = new Patient();
            patient.setAccountId(saved.getId());
            patient.setFullName(saved.getFullName());
            patient.setEmail(saved.getEmail());
            patient.setPhone(saved.getPhone());
            patient.setGender("unknown");
            patient.setCreatedAt(OffsetDateTime.now());
            patient.setUpdatedAt(OffsetDateTime.now());
            patientRepository.save(patient);
        }
        return adminLookupService.accountMap(saved);
    }

    @Transactional
    public Map<String, Object> updateAccount(int id, Map<String, Object> body) {
        Account a = accountRepository.findById(id).orElseThrow(() -> new ApiException(404, "Không tìm thấy tài khoản"));
        if (body.get("roleId") != null) {
            Role role = roleRepository.findById(intVal(body.get("roleId")))
                    .orElseThrow(() -> new ApiException(400, "Role không hợp lệ"));
            a.setRole(role);
        }
        if (body.containsKey("email")) a.setEmail(str(body.get("email")));
        if (body.containsKey("phone")) a.setPhone(str(body.get("phone")));
        if (body.containsKey("fullName")) a.setFullName(requireStr(body.get("fullName"), "fullName"));
        if (body.get("password") != null && !str(body.get("password")).isBlank()) {
            a.setPassword(str(body.get("password")));
        }
        if (body.containsKey("status")) a.setStatus(str(body.get("status")));
        a.setUpdatedAt(OffsetDateTime.now());
        return adminLookupService.accountMap(accountRepository.save(a));
    }

    @Transactional
    public Map<String, Object> blockAccount(int id) {
        Account a = accountRepository.findById(id).orElseThrow(() -> new ApiException(404, "Không tìm thấy tài khoản"));
        a.setStatus("blocked");
        a.setUpdatedAt(OffsetDateTime.now());
        return adminLookupService.accountMap(accountRepository.save(a));
    }

    @Transactional
    public Map<String, Object> unblockAccount(int id) {
        Account a = accountRepository.findById(id).orElseThrow(() -> new ApiException(404, "Không tìm thấy tài khoản"));
        a.setStatus("active");
        a.setUpdatedAt(OffsetDateTime.now());
        return adminLookupService.accountMap(accountRepository.save(a));
    }

    // --- Doctors ---
    @Transactional
    public DoctorAdminDto createDoctor(Map<String, Object> body) {
        Doctor d = new Doctor();
        applyDoctor(d, body);
        d.setCreatedAt(OffsetDateTime.now());
        d.setUpdatedAt(OffsetDateTime.now());
        d = doctorRepository.save(d);
        int doctorId = d.getId();
        replaceSpecialties(doctorId, body.get("specialtyIds"));
        return adminDoctorService.list().stream().filter(x -> x.getDoctorId().equals(doctorId)).findFirst()
                .orElseThrow(() -> new ApiException(500, "Không tải được bác sĩ vừa tạo"));
    }

    @Transactional
    public DoctorAdminDto updateDoctor(int id, Map<String, Object> body) {
        Doctor d = doctorRepository.findById(id).orElseThrow(() -> new ApiException(404, "Không tìm thấy bác sĩ"));
        applyDoctor(d, body);
        d.setUpdatedAt(OffsetDateTime.now());
        doctorRepository.save(d);
        if (body.containsKey("specialtyIds")) {
            replaceSpecialties(id, body.get("specialtyIds"));
        }
        return adminDoctorService.list().stream().filter(x -> x.getDoctorId().equals(id)).findFirst()
                .orElseThrow(() -> new ApiException(500, "Không tải được bác sĩ"));
    }

    @Transactional
    public void deleteDoctor(int id) {
        doctorRepository.deleteById(id);
    }

    // --- Slots ---
    @Transactional
    public List<Map<String, Object>> generateSlots(Map<String, Object> body) {
        int doctorId = intVal(body.get("doctorId"));
        LocalDate date = LocalDate.parse(requireStr(body.get("date"), "date"));
        LocalTime start = LocalTime.parse(requireStr(body.get("startTime"), "startTime"));
        LocalTime end = LocalTime.parse(requireStr(body.get("endTime"), "endTime"));
        int slotMinutes = intVal(body.get("slotMinutes")) > 0 ? intVal(body.get("slotMinutes")) : 30;
        int capacity = intVal(body.get("capacity")) > 0 ? intVal(body.get("capacity")) : 1;
        Integer roomId = body.get("roomId") != null ? intVal(body.get("roomId")) : null;

        List<AppointmentSlot> created = new ArrayList<>();
        LocalTime cursor = start;
        ZoneOffset offset = ZoneOffset.ofHours(7);
        while (cursor.plusMinutes(slotMinutes).compareTo(end) <= 0) {
            LocalTime next = cursor.plusMinutes(slotMinutes);
            AppointmentSlot slot = new AppointmentSlot();
            slot.setDoctorId(doctorId);
            slot.setRoomId(roomId);
            slot.setStartsAt(date.atTime(cursor).atOffset(offset));
            slot.setEndsAt(date.atTime(next).atOffset(offset));
            slot.setCapacity(capacity);
            slot.setBookedCount(0);
            slot.setActive(true);
            slot.setCreatedAt(OffsetDateTime.now());
            created.add(slotRepository.save(slot));
            cursor = next;
        }
        return created.stream().map(this::slotMap).toList();
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listSlots(int doctorId, String date) {
        LocalDate day = LocalDate.parse(date);
        ZoneOffset offset = ZoneOffset.ofHours(7);
        return slotRepository.findActiveByDoctorAndDay(
                doctorId,
                day.atStartOfDay().atOffset(offset),
                day.plusDays(1).atStartOfDay().atOffset(offset)
        ).stream().map(this::slotMap).toList();
    }

    @Transactional
    public Map<String, Object> deactivateSlot(int slotId) {
        AppointmentSlot slot = slotRepository.findById(slotId)
                .orElseThrow(() -> new ApiException(404, "Không tìm thấy slot"));
        slot.setActive(false);
        return slotMap(slotRepository.save(slot));
    }

    private Map<String, Object> slotMap(AppointmentSlot s) {
        Map<String, Object> m = new HashMap<>();
        m.put("slotId", s.getId());
        m.put("doctorId", s.getDoctorId());
        m.put("roomId", s.getRoomId());
        m.put("startsAt", s.getStartsAt().toString());
        m.put("endsAt", s.getEndsAt().toString());
        m.put("capacity", s.getCapacity());
        m.put("bookedCount", s.getBookedCount());
        m.put("isActive", s.getActive());
        m.put("createdAt", s.getCreatedAt() != null ? s.getCreatedAt().toString() : null);
        return m;
    }

    private void applyDoctor(Doctor d, Map<String, Object> body) {
        if (body.get("accountId") != null) d.setAccountId(intVal(body.get("accountId")));
        if (body.get("fullName") != null) d.setFullName(requireStr(body.get("fullName"), "fullName"));
        if (body.containsKey("gender")) d.setGender(str(body.get("gender")));
        if (body.get("dob") != null && !str(body.get("dob")).isBlank()) {
            d.setDob(LocalDate.parse(str(body.get("dob")).substring(0, 10)));
        }
        if (body.containsKey("phone")) d.setPhone(str(body.get("phone")));
        if (body.containsKey("email")) d.setEmail(str(body.get("email")));
        if (body.containsKey("licenseNo")) d.setLicenseNo(str(body.get("licenseNo")));
        if (body.containsKey("bio")) d.setBio(str(body.get("bio")));
        if (body.containsKey("avatarUrl")) d.setAvatarUrl(str(body.get("avatarUrl")));
        if (body.get("rating") != null) d.setRating(BigDecimal.valueOf(((Number) body.get("rating")).doubleValue()));
        if (body.get("visitsCount") != null) d.setVisitsCount(intVal(body.get("visitsCount")));
        if (body.containsKey("roomLocation")) d.setRoomLocation(str(body.get("roomLocation")));
        if (body.containsKey("scheduleText")) d.setScheduleText(str(body.get("scheduleText")));
        try {
            if (body.get("education") != null) {
                d.setEducationJson(objectMapper.writeValueAsString(body.get("education")));
            }
            if (body.get("certificates") != null) {
                d.setCertificatesJson(objectMapper.writeValueAsString(body.get("certificates")));
            }
        } catch (Exception ignored) {
        }
        if (d.getFullName() == null) d.setFullName("Bác sĩ");
        if (d.getGender() == null) d.setGender("unknown");
        if (d.getRating() == null) d.setRating(BigDecimal.valueOf(4.8));
        if (d.getVisitsCount() == null) d.setVisitsCount(0);
    }

    @SuppressWarnings("unchecked")
    private void replaceSpecialties(int doctorId, Object specialtyIdsObj) {
        List<DoctorSpecialty> existing = doctorSpecialtyRepository.findByDoctorIdIn(List.of(doctorId));
        doctorSpecialtyRepository.deleteAll(existing);
        if (specialtyIdsObj instanceof List<?> list) {
            for (Object o : list) {
                DoctorSpecialty ds = new DoctorSpecialty();
                ds.setDoctorId(doctorId);
                ds.setSpecialtyId(intVal(o));
                doctorSpecialtyRepository.save(ds);
            }
        }
    }

    private ClinicService mapService(ClinicService s, Map<String, Object> body) {
        if (body.get("specialtyId") != null) s.setSpecialtyId(intVal(body.get("specialtyId")));
        s.setCode(str(body.get("code")));
        s.setName(requireStr(body.get("name"), "name"));
        s.setDescription(str(body.get("description")));
        s.setDurationMinutes(intVal(body.get("durationMinutes")) > 0 ? intVal(body.get("durationMinutes")) : 15);
        s.setPriceCents(body.get("priceCents") != null ? ((Number) body.get("priceCents")).longValue() : 0L);
        s.setActive(body.get("isActive") == null || Boolean.TRUE.equals(body.get("isActive")));
        return s;
    }

    private static String str(Object o) {
        return o == null ? null : o.toString();
    }

    private static String requireStr(Object o, String field) {
        if (o == null || o.toString().isBlank()) throw new ApiException(400, field + " is required");
        return o.toString();
    }

    private static int intVal(Object o) {
        if (o == null) return 0;
        if (o instanceof Number n) return n.intValue();
        return Integer.parseInt(o.toString());
    }
}
