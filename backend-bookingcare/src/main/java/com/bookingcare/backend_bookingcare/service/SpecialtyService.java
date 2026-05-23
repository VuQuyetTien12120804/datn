package com.bookingcare.backend_bookingcare.service;

import com.bookingcare.backend_bookingcare.dto.SpecialtyDto;
import com.bookingcare.backend_bookingcare.entity.Specialty;
import com.bookingcare.backend_bookingcare.repository.SpecialtyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SpecialtyService {

    private final SpecialtyRepository specialtyRepository;

    @Transactional(readOnly = true)
    public List<SpecialtyDto> listAll() {
        return specialtyRepository.findAll().stream().map(this::toDto).toList();
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
