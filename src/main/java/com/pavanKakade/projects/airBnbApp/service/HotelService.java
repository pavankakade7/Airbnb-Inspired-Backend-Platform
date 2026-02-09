package com.pavanKakade.projects.airBnbApp.service;

import com.pavanKakade.projects.airBnbApp.dto.HotelDto;
import com.pavanKakade.projects.airBnbApp.dto.HotelInfoDto;
import com.pavanKakade.projects.airBnbApp.dto.HotelInfoRequestDto;

import java.util.List;

public interface HotelService {
    HotelDto createNewHotel(HotelDto hotelDto);

    HotelDto getHotelById(Long id);

    HotelDto updateHotelById(Long id, HotelDto hotelDto);

    void deleteHotelById(Long id);

    void activateHotel(Long hotelId);

    HotelInfoDto getHotelInfoById(Long hotelId, HotelInfoRequestDto hotelInfoRequestDto);

    List<HotelDto> getAllHotels();
}
