package com.project.realtimechat.dto;

import java.time.Instant;

import com.project.realtimechat.entity.EnumStatus;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MessageStatusDTO {
	private Long userReceivedId;
	private Long userSentId;
	private Long messageId;
    private EnumStatus status;
    private Instant timestamp;
}
