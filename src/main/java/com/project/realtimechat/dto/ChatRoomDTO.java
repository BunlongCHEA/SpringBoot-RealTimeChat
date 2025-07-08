package com.project.realtimechat.dto;

import java.util.Set;

import com.project.realtimechat.entity.EnumMessageType;
import com.project.realtimechat.entity.EnumRoomType;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChatRoomDTO {
	private Long id;
    private String name;
    private EnumRoomType type;
    private Set<ParticipantDTO> participants;
    private Long lastMessageId;
    private String lastMessageContent; // Added field for last message content
    private String lastMessageSenderUsername; // Added field for last message sender
    private String lastMessageTimestamp; // Added field for last message timestamp
    private EnumMessageType lastMessageType; // Added to store the type of the last message
    private Integer lastMessageAttachmentCount; // Added to store the number of attachments
}
