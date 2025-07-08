package com.project.realtimechat.service;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

import com.project.realtimechat.dto.BaseDTO;
import com.project.realtimechat.dto.UserDTO;
import com.project.realtimechat.entity.User;

public interface UserService extends UserDetailsService {
	// CRUD Operations
	ResponseEntity<BaseDTO<UserDTO>> getUserById(Long id);
	ResponseEntity<BaseDTO<UserDTO>> getUserByUsername(String username);
	ResponseEntity<BaseDTO<List<UserDTO>>> getAllUsers();
	ResponseEntity<BaseDTO<UserDTO>> createUser(UserDTO userDTO);
    ResponseEntity<BaseDTO<UserDTO>> updateUser(Long id, UserDTO userDTO);
    ResponseEntity<BaseDTO<Void>> deleteUser(Long id);
	
    // Helper Methods
	boolean existsByUsername(String username);
	//UserDetails loadUserByUsername(String username);
	User findEntityByIdUsers(Long id);
}
