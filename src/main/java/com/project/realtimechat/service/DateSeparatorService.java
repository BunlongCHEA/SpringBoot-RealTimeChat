package com.project.realtimechat.service;

import org.springframework.stereotype.Service;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.ArrayList;
import com.project.realtimechat.entity.ChatMessage;
import com.project.realtimechat.entity.ChatRoom;
import com.project.realtimechat.entity.EnumMessageType;

@Service
public class DateSeparatorService {
    
    // Use negative IDs starting from -1000 to avoid conflicts
    private final AtomicLong virtualIdGenerator = new AtomicLong(-1000L);
    
    public List<ChatMessage> insertDateSeparators(List<ChatMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            return messages;
        }
        
        List<ChatMessage> messagesWithSeparators = new ArrayList<>();
        ChatMessage previousMessage = null;
        
        for (ChatMessage message : messages) {
            // Skip existing system messages that are already date separators
            if (message.getType() == EnumMessageType.SYSTEM && 
                isExistingDateSeparator(message.getContent())) {
                // Don't add this system message, and don't use it for comparison
                continue;
            }
            
            // Check if we need to insert a date separator
            if (shouldInsertSeparator(previousMessage, message)) {
                ChatMessage separator = createDateSeparator(message.getTimestamp(), message.getChatRooms());
                messagesWithSeparators.add(separator);
            }
            
            messagesWithSeparators.add(message);
            previousMessage = message;
        }
        
        return messagesWithSeparators;
    }
    
    private boolean shouldInsertSeparator(ChatMessage previousMessage, ChatMessage currentMessage) {
        if (previousMessage == null) {
            return true; // First message needs a separator
        }
        
        long hoursDifference = ChronoUnit.HOURS.between(
            previousMessage.getTimestamp(), 
            currentMessage.getTimestamp()
        );
        
        return hoursDifference >= 2;
    }
    
    private boolean isExistingDateSeparator(String content) {
        return content != null && (
            content.equals("Chat started") ||
            content.contains("Today") ||
            content.contains("Yesterday") ||
            content.matches(".*\\d{2}:\\d{2}.*") ||
            content.matches(".*\\d{2}/\\d{2}/\\d{4}.*") ||
            content.matches(".*(Monday|Tuesday|Wednesday|Thursday|Friday|Saturday|Sunday).*")
        );
    }
    
    private ChatMessage createDateSeparator(Instant timestamp, ChatRoom chatRoom) {
        ChatMessage separator = new ChatMessage();
        separator.setChatRooms(chatRoom);
        separator.setType(EnumMessageType.SYSTEM);
        separator.setTimestamp(timestamp);
        separator.setContent(generateDateSeparatorText(timestamp));
        
        // Use unique negative ID to avoid conflicts
        separator.setId(virtualIdGenerator.getAndDecrement());
        
        return separator;
    }
    
    public String generateDateSeparatorText(Instant timestamp) {
        LocalDateTime messageTime = LocalDateTime.ofInstant(timestamp, ZoneId.systemDefault());
        LocalDateTime now = LocalDateTime.now();
        
        long daysDifference = ChronoUnit.DAYS.between(messageTime.toLocalDate(), now.toLocalDate());
        
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
        String timeStr = messageTime.format(timeFormatter);
        
        if (daysDifference == 0) {
            return "Today " + timeStr;
        } else if (daysDifference == 1) {
            return "Yesterday " + timeStr;
        } else if (daysDifference <= 7) {
            DateTimeFormatter dayFormatter = DateTimeFormatter.ofPattern("EEEE");
            return messageTime.format(dayFormatter) + " " + timeStr;
        } else {
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            return messageTime.format(dateFormatter) + " " + timeStr;
        }
    }
}