package com.bookingcare.backend_bookingcare.repository;

import com.bookingcare.backend_bookingcare.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface MessageRepository extends JpaRepository<Message, Integer> {

    List<Message> findByThreadIdOrderByCreatedAtAsc(Integer threadId);

    Optional<Message> findFirstByThreadIdOrderByCreatedAtDesc(Integer threadId);

    long countByThreadIdAndReadAtIsNullAndSenderAccountIdNot(Integer threadId, Integer senderAccountId);

    @Modifying
    @Query("""
            UPDATE Message m
            SET m.readAt = :readAt
            WHERE m.threadId = :threadId
              AND m.senderAccountId <> :accountId
              AND m.readAt IS NULL
            """)
    int markReadForThread(@Param("threadId") Integer threadId,
                          @Param("accountId") Integer accountId,
                          @Param("readAt") OffsetDateTime readAt);
}
