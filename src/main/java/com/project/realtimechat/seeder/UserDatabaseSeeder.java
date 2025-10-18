package com.project.realtimechat.seeder;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.project.realtimechat.entity.User;
import com.project.realtimechat.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserDatabaseSeeder {
	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	
	@EventListener
    @Transactional
    public void onApplicationReady(ApplicationReadyEvent event) {
        seedDatabase();
    }
	
    private void seedDatabase() {
        log.info("🌱 Starting database seeding...");
        
        seedUsers();
        
        log.info("✅ Database seeding completed!");
    }
    
    private void seedUsers() {
        // Skip if users already exist
        if (userRepository.existsByUsername("testuser1")) {
            log.info("Test users already exist, skipping user seeding");
            return;
        }

        log.info("Creating test users...");

        // User 1
        User user1 = createTestUser(
            "testuser1", 
            "Test User1", 
            "securePassword123", 
            "testuser1@example.com", 
            "https://example.com/avatars/default.png"
        );

        // User 2
        User user2 = createTestUser(
            "testuser2", 
            "Test User2", 
            "securePassword123", 
            "testuser2@example.com", 
            "https://example.com/avatars/default2.png"
        );

        // User 3
        User user3 = createTestUser(
            "testuser3", 
            "Test User3", 
            "securePassword123", 
            "testuser3@example.com", 
            "https://example.com/avatars/default3.png"
        );

        // Save users
        userRepository.save(user1);
        userRepository.save(user2);
        userRepository.save(user3);

        log.info("✅ Created 3 test users successfully");
    }
    
    private User createTestUser(String username, String fullName, String password, String email, String avatarUrl) {
        User user = new User();
        user.setUsername(username);
        user.setFullName(fullName);
        user.setPassword(passwordEncoder.encode(password));
        user.setEmail(email);
        user.setAvatarUrl(avatarUrl);
//        user.setEnabled(true);
//        user.setAccountNonExpired(true);
//        user.setAccountNonLocked(true);
//        user.setCredentialsNonExpired(true);
        
        User savedUser = userRepository.save(user);
        log.info("👤 Created user: {} (ID: {})", savedUser.getUsername(), savedUser.getId());
        
        return savedUser;
    }	
}