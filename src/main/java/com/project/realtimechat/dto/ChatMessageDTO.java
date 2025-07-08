package com.project.realtimechat.dto;

import java.time.Instant;
import java.util.Set;

import com.project.realtimechat.entity.EnumMessageType;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChatMessageDTO {
	private Long id;
    private Long chatRoomId;
    private Long senderId;
    private String senderName;
    private String content;
    private EnumMessageType type;
    private Instant timestamp;
    private Set<String> attachmentUrls;
    private Set<MessageStatusDTO> statuses;
}
