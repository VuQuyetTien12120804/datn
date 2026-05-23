package com.bookingcare.backend_bookingcare.service;

import com.bookingcare.backend_bookingcare.common.ApiException;
import com.bookingcare.backend_bookingcare.dto.SpecialtyDto;
import com.bookingcare.backend_bookingcare.dto.UpsertSpecialtyRequest;
import com.bookingcare.backend_bookingcare.entity.Specialty;
import com.bookingcare.backend_bookingcare.repository.SpecialtyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminSpecialtyService {

    private final SpecialtyRepository specialtyRepository;
    private final SpecialtyService specialtyService;

    @Transactional(readOnly = true)
    public List<SpecialtyDto> list() {
        return specialtyService.listAll();
    }

    @Transactional
    public SpecialtyDto create(UpsertSpecialtyRequest req) {
        Specialty s = new Specialty();
        s.setCode(req.getCode());
        s.setName(req.getName().trim());
        s.setDescription(req.getDescription());
        s.setCreatedAt(OffsetDateTime.now());
        return toDto(specialtyRepository.save(s));
    }

    @Transactional
    public SpecialtyDto update(int id, UpsertSpecialtyRequest req) {
        Specialty s = specialtyRepository.findById(id)
                .orElseThrow(() -> new ApiException(404, "Không tìm thấy chuyên khoa"));
        s.setCode(req.getCode());
        s.setName(req.getName().trim());
        s.setDescription(req.getDescription());
        return toDto(specialtyRepository.save(s));
    }

    @Transactional
    public void delete(int id) {
        if (!specialtyRepository.existsById(id)) {
            throw new ApiException(404, "Không tìm thấy chuyên khoa");
        }
        specialtyRepository.deleteById(id);
    }

    private SpecialtyDto toDto(Specialty s) {
        return SpecialtyDto.builder()
                .specialtyId(s.getId())
                .code(s.getCode())
                .name(s.getName())
                .description(s.getDescription())
                .createdAt(s.getCreatedAt() != null ? s.getCreatedAt().toString() : null)
                .build();
    }
}
