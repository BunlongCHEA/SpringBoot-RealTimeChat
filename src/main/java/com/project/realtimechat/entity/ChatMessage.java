package com.project.realtimechat.entity;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NonNull;
import lombok.Singular;

@Entity
@Data
@Table(name = "chat_messages")
public class ChatMessage {
	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "chat_room_id", nullable = false)
    private ChatRoom chatRooms;

    @ManyToOne
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @Size(max = 2048)
    private String content;

    @Enumerated(EnumType.STRING)
    @NotNull
    private EnumMessageType type; // Enumerable: TEXT, IMAGE, FILE, SYSTEM

    @NotNull
    private Instant timestamp;

    @ElementCollection
    @CollectionTable(name = "message_attachments", joinColumns = @JoinColumn(name = "message_id"))
    @Column(name = "url", length = 255)
    @Singular
    private Set<String> attachmentUrls;

    @OneToMany(mappedBy = "chatMessages")
    @Singular
    private Set<MessageStatus> statuses = new HashSet<>();
    
    @Transient
    private Long chatRoomId;

    @Transient
    private Long senderId;

    @Transient
    private String senderName;

    // Getters for transient fields

    public Long getChatRoomId() {
        return chatRooms != null ? chatRooms.getId() : null;
    }

    public Long getSenderId() {
        return sender != null ? sender.getId() : null;
    }

    public String getSenderName() {
        return sender != null ? sender.getUsername() : null;
    }
}
