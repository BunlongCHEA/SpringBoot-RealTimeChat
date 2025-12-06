package com.project.realtimechat.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.project.realtimechat.entity.User;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
	Optional<User> findByUsername(String username);
	
	boolean existsByUsername(String username);
	
    Optional<User> findById(Long id);
    
//    List<User> findByIsActive(boolean isActive);
}
