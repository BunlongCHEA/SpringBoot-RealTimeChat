package com.project.realtimechat.serviceImpl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.Notification;
import com.google.firebase.messaging.SendResponse;
import com.google.firebase.messaging.WebpushConfig;
import com.google.firebase.messaging.WebpushFcmOptions;
import com.google.firebase.messaging.WebpushNotification;
import com.project.realtimechat.dto.PushNotificationDTO;
import com.project.realtimechat.entity.ChatMessage;
import com.project.realtimechat.entity.EnumMessageType;
import com.project.realtimechat.entity.FcmToken;
import com.project.realtimechat.entity.User;
import com.project.realtimechat.repository.FcmTokenRepository;
import com.project.realtimechat.repository.UserRepository;
import com.project.realtimechat.service.PushNotificationService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class PushNotificationServiceImpl implements PushNotificationService {
    private final FirebaseMessaging firebaseMessaging;
    private final FcmTokenRepository fcmTokenRepository;
    private final UserRepository userRepository;

    @Value("${firebase.enabled:true}")
    private boolean firebaseEnabled;

    // Check if push notifications are available
    private boolean isPushNotificationAvailable() {
        if (!firebaseEnabled) {
            log.debug("Firebase is disabled in configuration");
            return false;
        }
        if (firebaseMessaging == null) {
            log.debug("FirebaseMessaging is not initialized");
            return false;
        }
        return true;
    }

    // --- Notification Sending ---
    @Override
    @Async
    public void sendNotification(String token, PushNotificationDTO notification) {
        if (!isPushNotificationAvailable()) {
            log.debug("Push notification skipped - Firebase not available");
            return;
        }

        try {
            Message message = buildMessage(token, notification);
            String response = firebaseMessaging.send(message);
            log.info("Push notification sent successfully: {}", response);
        } catch (FirebaseMessagingException e) {
            handleFirebaseException(token, e);
        }
    }

    @Override
    @Async
    public void sendNotificationToUser(Long userId, PushNotificationDTO notification) {
        if (!isPushNotificationAvailable()) {
            return;
        }

        List<FcmToken> tokens = fcmTokenRepository.findByUserIdAndIsActiveTrue(userId);
        
        if (tokens.isEmpty()) {
            log. debug("No active FCM tokens found for user {}", userId);
            return;
        }

        for (FcmToken fcmToken : tokens) {
            sendNotification(fcmToken.getToken(), notification);
        }
    }

    @Override
    @Async
    public void sendNotificationToUsers(List<Long> userIds, PushNotificationDTO notification) {
        if (!isPushNotificationAvailable()) {
            return;
        }

        if (userIds == null || userIds.isEmpty()) {
            return;
        }

        List<FcmToken> tokens = fcmTokenRepository.findActiveTokensByUserIds(userIds);
        
        if (tokens. isEmpty()) {
            log.debug("No active FCM tokens found for users: {}", userIds);
            return;
        }

        // Use batch sending for efficiency
        List<String> tokenStrings = tokens.stream()
                .map(FcmToken::getToken)
                .collect(Collectors.toList());

        sendBatchNotification(tokenStrings, notification);
    }

    @Override
    @Async
    public void sendNewMessageNotification(ChatMessage message, List<Long> recipientUserIds) {
        if (!isPushNotificationAvailable()) {
            return;
        }

        if (recipientUserIds == null || recipientUserIds.isEmpty()) {
            return;
        }

        String senderName = message.getSender().getFullName() != null 
                ? message.getSender().getFullName() 
                : message. getSender().getUsername();

        String chatRoomName = message.getChatRooms().getName();
        
        // Build notification content based on message type
        String notificationBody;
        if (message.getType() == EnumMessageType.IMAGE) {
            notificationBody = "Sent a photo";
        } else if (message.getType() == EnumMessageType.FILE) {
            notificationBody = "Sent a file";
        } else {
            // Truncate message if too long
            String content = message.getContent();
            notificationBody = content.length() > 100 
                    ? content.substring(0, 100) + "..." 
                    : content;
        }

        // Build notification data
        Map<String, String> data = new HashMap<>();
        data.put("type", "NEW_MESSAGE");
        data.put("chatRoomId", message.getChatRooms().getId().toString());
        data.put("messageId", message.getId().toString());
        data.put("senderId", message.getSender().getId().toString());
        data.put("messageType", message.getType().toString());
        data.put("timestamp", message.getTimestamp().toString());

        PushNotificationDTO notification = PushNotificationDTO.builder()
                .title(senderName + " • " + chatRoomName)
                .body(notificationBody)
                .clickAction("/chat/" + message.getChatRooms().getId())
                . data(data)
                .build();

        // Add image URL for image messages
        if (message. getType() == EnumMessageType.IMAGE && 
            message.getAttachmentUrls() != null && 
            ! message.getAttachmentUrls().isEmpty()) {
            notification. setImageUrl(message.getAttachmentUrls().iterator().next());
        }

        log.info("Sending push notification for message {} to {} recipients", 
                message.getId(), recipientUserIds.size());

        sendNotificationToUsers(recipientUserIds, notification);
    }

    // --- Token Management ---
    @Override
    @Transactional
    public void registerToken(Long userId, String token, String deviceType) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        // Check if token already exists
        Optional<FcmToken> existingToken = fcmTokenRepository.findByToken(token);
        
        if (existingToken.isPresent()) {
            FcmToken fcmToken = existingToken.get();
            
            // If token belongs to different user, reassign it
            if (!fcmToken. getUser().getId().equals(userId)) {
                fcmToken. setUser(user);
            }
            
            fcmToken.setActive(true);;
            fcmToken.setDeviceType(deviceType);
            fcmTokenRepository.save(fcmToken);
            log.info("Updated FCM token for user {}", userId);
        } else {
            // Create new token
            FcmToken fcmToken = new FcmToken();
            fcmToken.setUser(user);
            fcmToken.setToken(token);
            fcmToken.setDeviceType(deviceType != null ? deviceType : "WEB");
            fcmToken.setActive(true);
            fcmTokenRepository.save(fcmToken);
            log.info("Registered new FCM token for user {}", userId);
        }
    }

    @Override
    @Transactional
    public void unregisterToken(Long userId, String token) {
        fcmTokenRepository.deleteByUserIdAndToken(userId, token);
        log.info("Unregistered FCM token for user {}", userId);
    }

    @Override
    @Transactional
    public void unregisterAllTokens(Long userId) {
        List<FcmToken> tokens = fcmTokenRepository.findByUserIdAndIsActiveTrue(userId);
        tokens.forEach(token -> token.setActive(false));
        fcmTokenRepository.saveAll(tokens);
        log.info("Unregistered all FCM tokens for user {}", userId);
    }

    // --- Helper Methods ---
    private Message buildMessage(String token, PushNotificationDTO notification) {
        Notification.Builder notificationBuilder = Notification.builder()
                .setTitle(notification.getTitle())
                .setBody(notification.getBody());

        if (notification.getImageUrl() != null && !notification.getImageUrl().isEmpty()) {
            notificationBuilder. setImage(notification.getImageUrl());
        }

        Message.Builder messageBuilder = Message.builder()
                .setToken(token)
                .setNotification(notificationBuilder.build())
                .setWebpushConfig(WebpushConfig.builder()
                        .setNotification(WebpushNotification.builder()
                                .setTitle(notification.getTitle())
                                .setBody(notification.getBody())
                                .setIcon("/icons/notification-icon.png")
                                .setBadge("/icons/badge-icon.png")
                                .build())
                        .setFcmOptions(WebpushFcmOptions.builder()
                                .setLink(notification.getClickAction())
                                .build())
                        .build());

        // Add custom data
        if (notification.getData() != null && !notification.getData().isEmpty()) {
            messageBuilder.putAllData(notification.getData());
        }

        return messageBuilder. build();
    }

    private void sendBatchNotification(List<String> tokens, PushNotificationDTO notification) {
        if (tokens.isEmpty() || ! isPushNotificationAvailable()) {
            return;
        }

        try {
            Notification.Builder notificationBuilder = Notification.builder()
                    . setTitle(notification.getTitle())
                    .setBody(notification.getBody());

            if (notification.getImageUrl() != null) {
                notificationBuilder.setImage(notification.getImageUrl());
            }

            MulticastMessage. Builder multicastBuilder = MulticastMessage.builder()
                    .addAllTokens(tokens)
                    .setNotification(notificationBuilder.build())
                    . setWebpushConfig(WebpushConfig.builder()
                            .setNotification(WebpushNotification.builder()
                                    .setTitle(notification.getTitle())
                                    .setBody(notification.getBody())
                                    // .setIcon("/icons/notification-icon.png")
                                    .build())
                            .setFcmOptions(WebpushFcmOptions.builder()
                                    .setLink(notification.getClickAction())
                                    . build())
                            .build());

            if (notification.getData() != null) {
                multicastBuilder.putAllData(notification.getData());
            }

            BatchResponse response = firebaseMessaging.sendEachForMulticast(multicastBuilder.build());
            
            log.info("Batch notification sent: {} success, {} failure", 
                    response.getSuccessCount(), response.getFailureCount());


            // Log detailed failure information
            if (response.getFailureCount() > 0) {
                List<SendResponse> responses = response.getResponses();
                for (int i = 0; i < responses.size(); i++) {
                    SendResponse sendResponse = responses.get(i);
                    if (! sendResponse.isSuccessful()) {
                        FirebaseMessagingException exception = sendResponse.getException();
                        String token = tokens.get(i);
                        
                        // ✅ Log detailed error information
                        log.error("❌ Failed to send to token {}: ErrorCode={}, Message={}", 
                                token. substring(0, Math.min(20, token.length())) + "...",
                                exception.getMessagingErrorCode(),
                                exception.getMessage());
                        
                        // ✅ Log full stack trace for debugging
                        log.error("Full error details:", exception);
                    }
                }
                
                handleBatchFailures(tokens, response);
            }

            // // Handle failed tokens
            // if (response.getFailureCount() > 0) {
            //     handleBatchFailures(tokens, response);
            // }

        } catch (FirebaseMessagingException e) {
            log.error("Failed to send batch notification: {}", e.getMessage(), e);

            if (e. getMessagingErrorCode() != null) {
                log.error("❌ Firebase error code: {}", e.getMessagingErrorCode());
            }
            if (e.getCause() != null) {
                log.error("❌ Cause: {}", e.getCause().getMessage());
            }
        } catch (Exception e) {
            log.error("❌ Unexpected error in batch notification: {}", e.getMessage(), e);
        }
    }

    private void handleFirebaseException(String token, FirebaseMessagingException e) {
        String errorCode = e.getMessagingErrorCode() != null 
                ? e.getMessagingErrorCode().toString() 
                : "UNKNOWN";

        log.error("Failed to send push notification: {} - {}", errorCode, e.getMessage());

        // Deactivate invalid tokens
        if ("UNREGISTERED".equals(errorCode) || "INVALID_ARGUMENT". equals(errorCode)) {
            fcmTokenRepository.deactivateToken(token);
            log.info("Deactivated invalid FCM token");
        }
    }

    private void handleBatchFailures(List<String> tokens, BatchResponse response) {
        List<SendResponse> responses = response.getResponses();
        
        for (int i = 0; i < responses.size(); i++) {
            SendResponse sendResponse = responses.get(i);
            
            if (! sendResponse.isSuccessful()) {
                FirebaseMessagingException exception = sendResponse.getException();
                String errorCode = exception.getMessagingErrorCode() != null 
                        ? exception.getMessagingErrorCode().toString() 
                        : "UNKNOWN";

                if ("UNREGISTERED".equals(errorCode) || "INVALID_ARGUMENT".equals(errorCode)) {
                    String failedToken = tokens.get(i);
                    fcmTokenRepository.deactivateToken(failedToken);
                    log.info("Deactivated invalid FCM token: {}...", 
                            failedToken. substring(0, Math.min(20, failedToken.length())));
                }
            }
        }
    }

}
