package com.pavanKakade.projects.airBnbApp.repository;

import com.pavanKakade.projects.airBnbApp.entity.Room;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomRepository extends JpaRepository<Room, Long> {
}
