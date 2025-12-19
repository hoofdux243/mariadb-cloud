package com.cloud_computing.mariadb.service.impl;

import com.cloud_computing.mariadb.dto.UserDTO;
import com.cloud_computing.mariadb.dto.response.AuthResponse;
import com.cloud_computing.mariadb.entity.User;
import com.cloud_computing.mariadb.exception.BadRequestException;
import com.cloud_computing.mariadb.responsitory.UserRepository;
import com.cloud_computing.mariadb.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthServiceImpl implements AuthService {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private JWTService jwtService;

    @Override
    public UserDTO register(UserDTO loginRequest) {
        if (userRepository.existsByUsername(loginRequest.getUsername()))
            throw new BadRequestException("Tên tài khoản đã tồn tại");
        if (loginRequest.getName() == null || loginRequest.getPassword().trim().isEmpty())
            throw new BadRequestException("Tên người dùng không được bỏ trống.");
        User user = User.builder()
                .username(loginRequest.getUsername())
                .password(passwordEncoder.encode(loginRequest.getPassword()))
                .name(loginRequest.getName())
                .build();
        userRepository.save(user);
        return loginRequest;
    }

    @Override
    public AuthResponse login(UserDTO loginRequest) {
        User user = userRepository.findByUsername(loginRequest.getUsername().trim()).orElseThrow(() -> new BadRequestException("Không tìm thấy người dùng"));
        if(!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())){
            throw new BadRequestException("Mật khẩu không đúng!");
        }
        var token = jwtService.generateToken(user);
        return AuthResponse.builder().token(token).build();
    }
}
