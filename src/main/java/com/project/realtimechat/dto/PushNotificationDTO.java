package com.project.realtimechat.dto;

import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PushNotificationDTO {
    private String title;
    private String body;
    private String imageUrl;
    private String clickAction;
    private Map<String, String> data;
}