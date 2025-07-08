package com.project.realtimechat.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.project.realtimechat.config.JwtService;
import com.project.realtimechat.dto.BaseDTO;
import com.project.realtimechat.dto.LoginRequest;
import com.project.realtimechat.dto.LoginResponse;
import com.project.realtimechat.dto.RefreshTokenRequest;
import com.project.realtimechat.dto.TokenRefreshResponse;
import com.project.realtimechat.dto.UserDTO;
import com.project.realtimechat.exception.BadRequestException;
import com.project.realtimechat.service.AuthService;
import com.project.realtimechat.service.UserService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    
    @Autowired
    private AuthService authService;
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private JwtService jwtService;
    
    /**
     * User registration endpoint - publicly accessible
     */
    @PostMapping("/register")
    public ResponseEntity<BaseDTO<UserDTO>> register(@Valid @RequestBody UserDTO userDTO) {
        return authService.registerUser(userDTO);
    }
    
    /**
     * User login endpoint - returns JWT token
     */
    @PostMapping("/login")
    public ResponseEntity<BaseDTO<LoginResponse>> login(@Valid @RequestBody LoginRequest loginRequest) {
        return authService.authenticateUser(loginRequest);
    }
    
    /**
     * Token refresh endpoint - get a new access token using a refresh token
     */
    @PostMapping("/refresh-token")
    public ResponseEntity<BaseDTO<TokenRefreshResponse>> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        try {
            String refreshToken = request.getRefreshToken();
            
            // Validate refresh token
            if (!jwtService.validateToken(refreshToken) || !jwtService.isRefreshToken(refreshToken)) {
                throw new BadRequestException("Invalid refresh token");
            }
            
            // Get username from token
            String username = jwtService.extractUsername(refreshToken);
            
            // Load user details
            UserDetails userDetails = userService.loadUserByUsername(username);
            
            // Generate new access token
            String newAccessToken = jwtService.generateToken(userDetails);
            
            // Create response
            TokenRefreshResponse response = new TokenRefreshResponse(newAccessToken, refreshToken);
            
            BaseDTO<TokenRefreshResponse> baseResponse = new BaseDTO<>(
                    200,
                    "Token refreshed successfully",
                    response);
            
            return ResponseEntity.ok(baseResponse);
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            throw new BadRequestException("Failed to refresh token: " + e.getMessage());
        }
    }
}