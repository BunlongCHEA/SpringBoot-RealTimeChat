package com.project.realtimechat.service;

import org.springframework.http.ResponseEntity;

import com.project.realtimechat.dto.BaseDTO;
import com.project.realtimechat.dto.LoginRequest;
import com.project.realtimechat.dto.LoginResponse;
import com.project.realtimechat.dto.UserDTO;

public interface AuthService {
    ResponseEntity<BaseDTO<UserDTO>> registerUser(UserDTO userDTO);
    ResponseEntity<BaseDTO<LoginResponse>> authenticateUser(LoginRequest loginRequest);
}