package com.cloud_computing.mariadb.controller;

import com.cloud_computing.mariadb.dto.UserDTO;
import com.cloud_computing.mariadb.dto.response.APIResponseMessage;
import com.cloud_computing.mariadb.dto.response.APIResponse;
import com.cloud_computing.mariadb.dto.response.AuthResponse;
import com.cloud_computing.mariadb.service.AuthService;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@FieldDefaults(level = AccessLevel.PRIVATE)
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    @Autowired
    AuthService authService;

    @PostMapping("/register")
    ResponseEntity<?> registerUser(@RequestBody UserDTO user) {
        UserDTO userDTO = authService.register(user);
        APIResponse apiResponse = APIResponse.builder()
                .code(HttpStatus.CREATED.value())
                .message(APIResponseMessage.SUCCESSFULLY_CREATED.name())
                .data(userDTO)
                .build();
        return new ResponseEntity<>(apiResponse, HttpStatus.CREATED);
    }

    @PostMapping("/login")
    ResponseEntity<?> login(@RequestBody UserDTO user) {
        AuthResponse authResponse = authService.login(user);
        APIResponse apiResponse = APIResponse.builder()
                .code(HttpStatus.OK.value())
                .message(APIResponseMessage.SUCCESSFULLY_LOGIN.name())
                .data(authResponse)
                .build();
        return new ResponseEntity<>(apiResponse, HttpStatus.OK);
    }
}
