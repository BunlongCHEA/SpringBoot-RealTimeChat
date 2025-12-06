package com.project.realtimechat.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.project.realtimechat.dto.BaseDTO;
import com.project.realtimechat.dto.FcmTokenRequest;
import com.project.realtimechat.entity.User;
import com.project.realtimechat.repository.UserRepository;
import com.project.realtimechat.service.PushNotificationService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/fcm-tokens")
@RequiredArgsConstructor
@Slf4j
public class FcmTokenController {
    private final PushNotificationService pushNotificationService;
    private final UserRepository userRepository;

    @PostMapping("/register")
    public ResponseEntity<BaseDTO<String>> registerToken(
            @RequestBody FcmTokenRequest request,
            Authentication authentication) {
        try {
            Long userId = getUserIdFromAuthentication(authentication);
            
            pushNotificationService.registerToken(
                    userId, 
                    request.getToken(), 
                    request.getDeviceType()
            );

            return ResponseEntity.ok(new BaseDTO<>(
                    HttpStatus.OK.value(),
                    "FCM token registered successfully",
                    "Token registered"
            ));
        } catch (Exception e) {
            log.error("Failed to register FCM token: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(new BaseDTO<>(
                    HttpStatus.BAD_REQUEST.value(),
                    "Failed to register FCM token: " + e.getMessage(),
                    null
            ));
        }
    }

    @DeleteMapping("/unregister")
    public ResponseEntity<BaseDTO<String>> unregisterToken(
            @RequestBody FcmTokenRequest request,
            Authentication authentication) {
        try {
            Long userId = getUserIdFromAuthentication(authentication);
            
            pushNotificationService.unregisterToken(userId, request.getToken());

            return ResponseEntity.ok(new BaseDTO<>(
                    HttpStatus.OK.value(),
                    "FCM token unregistered successfully",
                    "Token unregistered"
            ));
        } catch (Exception e) {
            log.error("Failed to unregister FCM token: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(new BaseDTO<>(
                    HttpStatus.BAD_REQUEST.value(),
                    "Failed to unregister FCM token: " + e.getMessage(),
                    null
            ));
        }
    }

    @DeleteMapping("/unregister-all")
    public ResponseEntity<BaseDTO<String>> unregisterAllTokens(Authentication authentication) {
        try {
            Long userId = getUserIdFromAuthentication(authentication);
            
            pushNotificationService.unregisterAllTokens(userId);

            log.info("All FCM tokens unregistered for user {}", userId);

            return ResponseEntity.ok(new BaseDTO<>(
                    HttpStatus.OK.value(),
                    "All FCM tokens unregistered successfully",
                    "All tokens unregistered"
            ));
        } catch (Exception e) {
            log.error("Failed to unregister all FCM tokens: {}", e.getMessage(), e);
            return ResponseEntity. badRequest().body(new BaseDTO<>(
                    HttpStatus. BAD_REQUEST.value(),
                    "Failed to unregister FCM tokens: " + e.getMessage(),
                    null
            ));
        }
    }

    private Long getUserIdFromAuthentication(Authentication authentication) {
        if (authentication == null) {
            throw new RuntimeException("Authentication required");
        }

        String username = authentication.getName();
        
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
        
        return user.getId();
    }


}
