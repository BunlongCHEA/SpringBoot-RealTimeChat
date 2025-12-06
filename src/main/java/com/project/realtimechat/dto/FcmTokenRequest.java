package com.project.realtimechat.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FcmTokenRequest {
    private String token;
    private String deviceType; // WEB, ANDROID, IOS   
}