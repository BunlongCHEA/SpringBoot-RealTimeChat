package com.project.realtimechat.dto;

import java.time.Instant;

import com.project.realtimechat.entity.EnumRoomRole;

import jakarta.persistence.Column;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserDTO {
	private Long id;
    private String username;
    private String fullName;
    private String avatarUrl;
    private String password;
    private String email;
    private boolean isActive = true;
    private boolean isLocked = false;
    private Instant lastLogin;
    private Instant createdAt;
    private Instant updatedAt;
}
