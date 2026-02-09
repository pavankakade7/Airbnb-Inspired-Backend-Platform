package com.pavanKakade.projects.airBnbApp.service;

import com.pavanKakade.projects.airBnbApp.dto.ProfileUpdateRequestDto;
import com.pavanKakade.projects.airBnbApp.dto.UserDto;
import com.pavanKakade.projects.airBnbApp.entity.User;

public interface UserService {

    User getUserById(Long id);

    void updateProfile(ProfileUpdateRequestDto profileUpdateRequestDto);

    UserDto getMyProfile();
}
