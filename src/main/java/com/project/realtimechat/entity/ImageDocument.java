package com.project.realtimechat.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "images")
public class ImageDocument {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String filename;
    private String contentType;
    private long size;
    private byte[] data;
    private String uploadedBy;
    private Instant uploadedAt;
    private Long chatRoomId;
    private Long messageId; // Reference to the chat message
}
