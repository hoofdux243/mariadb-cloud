package com.cloud_computing.mariadb.service;

import com.cloud_computing.mariadb.dto.UserDTO;
import com.cloud_computing.mariadb.dto.response.AuthResponse;


public interface AuthService {
    UserDTO register(UserDTO loginRequest);
    AuthResponse login(UserDTO loginRequest);
}
