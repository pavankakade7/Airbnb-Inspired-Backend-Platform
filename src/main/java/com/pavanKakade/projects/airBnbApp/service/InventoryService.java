package com.pavanKakade.projects.airBnbApp.service;

import com.pavanKakade.projects.airBnbApp.dto.HotelPriceResponseDto;
import com.pavanKakade.projects.airBnbApp.dto.HotelSearchRequest;
import com.pavanKakade.projects.airBnbApp.dto.InventoryDto;
import com.pavanKakade.projects.airBnbApp.dto.UpdateInventoryRequestDto;
import com.pavanKakade.projects.airBnbApp.entity.Room;
import org.springframework.data.domain.Page;

import java.util.List;

public interface InventoryService {

    void initializeRoomForAYear(Room room);

    void deleteAllInventories(Room room);

    Page<HotelPriceResponseDto> searchHotels(HotelSearchRequest hotelSearchRequest);

    List<InventoryDto> getAllInventoryByRoom(Long roomId);

    void updateInventory(Long roomId, UpdateInventoryRequestDto updateInventoryRequestDto);
}
