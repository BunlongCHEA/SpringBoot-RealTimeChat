package com.project.realtimechat.service;

import java.util.List;

import com.project.realtimechat.dto.PushNotificationDTO;
import com.project.realtimechat.entity.ChatMessage;

public interface PushNotificationService {
    void sendNotification(String token, PushNotificationDTO notification);
    
    void sendNotificationToUser(Long userId, PushNotificationDTO notification);
    
    void sendNotificationToUsers(List<Long> userIds, PushNotificationDTO notification);
    
    void sendNewMessageNotification(ChatMessage message, List<Long> recipientUserIds);
    
    void registerToken(Long userId, String token, String deviceType);
    
    void unregisterToken(Long userId, String token);
    
    void unregisterAllTokens(Long userId);    
}
