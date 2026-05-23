package com.bookingcare.backend_bookingcare.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter
@Setter
@Entity
@Table(name = "app_legal_documents", schema = "dbo")
public class AppLegalDocument {

    @Id
    private Integer id;

    @Column(nullable = false, length = 32)
    private String code;

    @Column(nullable = false)
    private String title;

    @Column(name = "body_html", nullable = false, columnDefinition = "nvarchar(max)")
    private String bodyHtml;

    private Integer version;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}
