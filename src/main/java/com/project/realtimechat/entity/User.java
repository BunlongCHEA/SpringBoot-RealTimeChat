package com.project.realtimechat.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Entity
@Data
@Table(name = "users")
public class User {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	@NotBlank
	@Size(min = 1, max = 30)
	@Column(unique = true)
    private String username;
	
	@Size(max = 50)
    private String fullName;
	
	@Size(max = 255)
    private String avatarUrl;
	
	@NotBlank
    @Size(max = 120)
    private String password;
    
    @Email
    @Size(max = 100)
    @Column(unique = true)
    private String email;
    
    @Column(name = "is_active")
    private boolean isActive = true;
    
    @Column(name = "is_locked")
    private boolean isLocked = false;
    
    @Column(name = "last_login")
    private Instant lastLogin;
    
    @Column(name = "created_at")
    private Instant createdAt;
    
    @Column(name = "updated_at")
    private Instant updatedAt;
}
