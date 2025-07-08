package com.project.realtimechat.entity;

import java.time.Instant;
import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Entity
@Data
@Table(name = "message_status")
public class MessageStatus {
	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
	
	@ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User users;
	
	@ManyToOne
    @JoinColumn(name = "message_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private ChatMessage chatMessages;

	@Enumerated(EnumType.STRING)
    @NotNull
    private EnumStatus status; // Enumerable: SENT, DELIVERED, READ

    @NotNull
    private Instant timestamp;
    
    
    @Transient
    private Long userId;

    @Transient
    private Long messageId;

    // Getters for transient fields

    public Long getUserId() {
        return users != null ? users.getId() : null;
    }

    public Long getMessageId() {
        return chatMessages != null ? chatMessages.getId() : null;
    }
}

