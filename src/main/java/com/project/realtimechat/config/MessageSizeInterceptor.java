package com.project.realtimechat.config;

import java.time.Instant;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class MessageSizeInterceptor implements ChannelInterceptor {

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        
        if (accessor != null) {
            String destination = accessor.getDestination();
            StompCommand command = accessor.getCommand();
            String sessionId = accessor.getSessionId();
            
            // Get payload size
            Object payload = message.getPayload();
            int payloadSize = 0;
            
            if (payload instanceof byte[]) {
                payloadSize = ((byte[]) payload).length;
            } else if (payload instanceof String) {
                payloadSize = ((String) payload).length();
            }
            
            double sizeInKB = payloadSize / 1024.0;
            double sizeInMB = payloadSize / (1024.0 * 1024.0);
            
            // Log ALL SEND commands, especially to /app/chat.sendImage
            if (StompCommand.SEND.equals(command)) {
                if (destination != null && destination.contains("/chat.sendImage")) {
                    log.info("=".repeat(80));
                    log.info("IMAGE MESSAGE INTERCEPTED!");
                    log.info("Command: {}", command);
                    log.info("Destination: {}", destination);
                    log.info("SessionId: {}", sessionId);
                    log.info("Payload Size: {:.2f}KB ({:.2f}MB) - {} bytes", sizeInKB, sizeInMB, payloadSize);
                    log.info("User: {}", accessor.getUser() != null ? accessor.getUser().getName() : "ANONYMOUS");
                    log.info("=".repeat(80));
                } else {
                    log.debug("Message - Command: {}, Destination: {}, Size: {:.2f}KB", command, destination, sizeInKB);
                }
            } else if (payloadSize > 10000) { // Log other large messages
                log.info("Large message detected - Command: {}, Destination: {}, Size: {:.2f}KB ({} bytes)", command, destination, sizeInKB, payloadSize);
            }
        }
        
        return message;
    }

    @Override
    public void postSend(Message<?> message, MessageChannel channel, boolean sent) {
        if (!sent) {
            StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
            if (accessor != null) {
                String destination = accessor.getDestination();
                if (destination != null && destination.contains("/chat.sendImage")) {
                    log.error("IMAGE MESSAGE FAILED TO SEND! Destination: {}",  destination);
                }
            }
        }
    }

    @Override
    public void afterSendCompletion(Message<?> message, MessageChannel channel, boolean sent, Exception ex) {
        if (ex != null) {
            StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
            if (accessor != null) {
                String destination = accessor.getDestination();
                log.error("Error after sending message to {}: {}", 
                        destination, ex.getMessage(), ex);
            }
        }
    }
}