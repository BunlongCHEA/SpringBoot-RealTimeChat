package com.project.realtimechat.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.project.realtimechat.dto.BaseDTO;
import com.project.realtimechat.dto.UserDTO;
import com.project.realtimechat.service.UserService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/users")
public class UserController {
	@Autowired
	private UserService userService;
	
	@GetMapping
	public ResponseEntity<BaseDTO<List<UserDTO>>> getAllUsers() {
		return userService.getAllUsers();
	}
	
	@GetMapping("/{id}")
	public ResponseEntity<BaseDTO<UserDTO>> getUserById(@PathVariable Long id) {
		return userService.getUserById(id);
	}
	
	@GetMapping("/username")
	public ResponseEntity<BaseDTO<UserDTO>> getUserByUsername(@RequestParam String username) {
		return userService.getUserByUsername(username);
	}
	
	@PostMapping
	public ResponseEntity<BaseDTO<UserDTO>> createUser(
            @Valid @RequestBody UserDTO userDTO) {
        return userService.createUser(userDTO);
    }
	
	@PutMapping("/{id}")
	public ResponseEntity<BaseDTO<UserDTO>> updateUser(@PathVariable Long id, @Valid @RequestBody UserDTO userDTO) {
		return userService.updateUser(id, userDTO);
	}
	
	@DeleteMapping("/{id}")
	public ResponseEntity<BaseDTO<Void>> deleteUser(@PathVariable Long id) {
		return userService.deleteUser(id);
	}
	
}
