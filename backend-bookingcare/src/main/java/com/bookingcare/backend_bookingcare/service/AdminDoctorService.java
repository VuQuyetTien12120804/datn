package com.bookingcare.backend_bookingcare.service;

import com.bookingcare.backend_bookingcare.dto.DoctorAdminDto;
import com.bookingcare.backend_bookingcare.entity.Doctor;
import com.bookingcare.backend_bookingcare.entity.DoctorSpecialty;
import com.bookingcare.backend_bookingcare.repository.DoctorRepository;
import com.bookingcare.backend_bookingcare.repository.DoctorSpecialtyRepository;
import com.bookingcare.backend_bookingcare.util.JsonStringListParser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminDoctorService {

    private final DoctorRepository doctorRepository;
    private final DoctorSpecialtyRepository doctorSpecialtyRepository;

    @Transactional(readOnly = true)
    public List<DoctorAdminDto> list() {
        List<Doctor> doctors = doctorRepository.findAll();
        List<Integer> ids = doctors.stream().map(Doctor::getId).toList();
        Map<Integer, List<Integer>> specMap = ids.isEmpty()
                ? Map.of()
                : doctorSpecialtyRepository.findByDoctorIdIn(ids).stream()
                .collect(Collectors.groupingBy(
                        DoctorSpecialty::getDoctorId,
                        Collectors.mapping(DoctorSpecialty::getSpecialtyId, Collectors.toList())
                ));
        List<DoctorAdminDto> out = new ArrayList<>();
        for (Doctor d : doctors) {
            out.add(toDto(d, specMap.getOrDefault(d.getId(), List.of())));
        }
        return out;
    }

    private DoctorAdminDto toDto(Doctor d, List<Integer> specialtyIds) {
        return DoctorAdminDto.builder()
                .doctorId(d.getId())
                .accountId(d.getAccountId())
                .fullName(d.getFullName())
                .gender(d.getGender())
                .dob(d.getDob() != null ? d.getDob().toString() : null)
                .phone(d.getPhone())
                .email(d.getEmail())
                .licenseNo(d.getLicenseNo())
                .bio(d.getBio())
                .avatarUrl(d.getAvatarUrl())
                .rating(d.getRating() != null ? d.getRating().doubleValue() : null)
                .visitsCount(d.getVisitsCount())
                .roomLocation(d.getRoomLocation())
                .scheduleText(d.getScheduleText())
                .education(JsonStringListParser.parse(d.getEducationJson()))
                .certificates(JsonStringListParser.parse(d.getCertificatesJson()))
                .createdAt(d.getCreatedAt() != null ? d.getCreatedAt().toString() : null)
                .updatedAt(d.getUpdatedAt() != null ? d.getUpdatedAt().toString() : null)
                .specialtyIds(specialtyIds)
                .build();
    }
}
