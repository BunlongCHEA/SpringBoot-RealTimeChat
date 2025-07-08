package com.project.realtimechat.entity;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Singular;
import lombok.ToString;

@Entity
@Data
@Table(name = "chat_rooms")
public class ChatRoom {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	@NotBlank
    @Size(max = 50)
    private String name;

    @Enumerated(EnumType.STRING)
    private EnumRoomType type; // Enumerable: PERSONAL, GROUP, CHANNEL

//    Stores additional metadata about a user's membership in a room like Role permissions (ADMIN/MEMBER), mute status, block status
    @OneToMany(mappedBy = "chatRooms", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @Singular
    private Set<Participant> participants = new HashSet<>();
    
    @ManyToOne
    @JoinColumn(name = "created_user_id", nullable = true)  
    private User createdUserId;

    @OneToOne
    @JoinColumn(name = "last_message_id")
    private ChatMessage chatMessages;
    
    // Transient fields - not stored in database, computed from chatMessages relationship
    @Transient
    private String lastMessageContent;
    
    @Transient
    private String lastMessageSenderUsername;
    
    @Transient
    private Instant lastMessageTimestamp;
    
    @Transient
    private EnumMessageType lastMessageType;
    
    @Transient
    private Integer lastMessageAttachmentCount;
    
    // Getter methods that dynamically fetch data from the chatMessages relationship
    public Long getLastMessageId() {
        return chatMessages != null ? chatMessages.getId() : null;
    }
    
    public String getLastMessageContent() {
        return chatMessages != null ? chatMessages.getContent() : null;
    }
    
    public String getLastMessageSenderUsername() {
        if (chatMessages != null && chatMessages.getSender() != null) {
            return chatMessages.getSender().getUsername();
        }
        return null;
    }
    
    public Instant getLastMessageTimestamp() {
        if (chatMessages != null && chatMessages.getTimestamp() != null) {
            return chatMessages.getTimestamp();
        }
        return null;
    }
    
    public Instant getLastMessageTimestampInstant() {
        return chatMessages != null ? chatMessages.getTimestamp() : null;
    }
    
    public EnumMessageType getLastMessageType() {
        return chatMessages != null ? chatMessages.getType() : null;
    }
    
    public Integer getLastMessageAttachmentCount() {
        if (chatMessages != null && chatMessages.getAttachmentUrls() != null) {
            return chatMessages.getAttachmentUrls().size();
        }
        return 0;
    }
}
