package com.bookingcare.backend_bookingcare.repository;

import com.bookingcare.backend_bookingcare.entity.Room;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomRepository extends JpaRepository<Room, Integer> {
}
