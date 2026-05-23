package com.bookingcare.backend_bookingcare.service;

import com.bookingcare.backend_bookingcare.common.ApiException;
import com.bookingcare.backend_bookingcare.entity.AppLegalDocument;
import com.bookingcare.backend_bookingcare.repository.AppLegalDocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class LegalService {

    private final AppLegalDocumentRepository repository;

    @Transactional(readOnly = true)
    public Map<String, Object> getByCode(String code) {
        AppLegalDocument doc = repository.findByCodeIgnoreCase(code)
                .orElseThrow(() -> new ApiException(404, "Không tìm thấy tài liệu"));
        Map<String, Object> m = new HashMap<>();
        m.put("code", doc.getCode());
        m.put("title", doc.getTitle());
        m.put("bodyHtml", doc.getBodyHtml());
        m.put("version", doc.getVersion() != null ? doc.getVersion() : 1);
        m.put("updatedAt", doc.getUpdatedAt() != null ? doc.getUpdatedAt().toString() : null);
        return m;
    }
}
