package com.project.realtimechat.serviceImpl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.realtimechat.dto.BaseDTO;
import com.project.realtimechat.dto.UserDTO;
import com.project.realtimechat.entity.User;
import com.project.realtimechat.exception.ResourceNotFoundException;
import com.project.realtimechat.exception.BadRequestException;
import com.project.realtimechat.repository.UserRepository;
import com.project.realtimechat.service.UserService;

@Service
public class UserServiceImpl implements UserService{
	@Autowired
	private UserRepository userRepository;
	
	@Autowired
	private ModelMapper modelMapper;
	
	/**
	 * Retrieves a user by their unique identifier
	 * @param id The unique identifier of the user to retrieve
	 */
	@Override
	@Transactional(readOnly = true)
	public ResponseEntity<BaseDTO<UserDTO>> getUserById(Long id) {
		try {
			// Retrieve the users entity by ID
			User user = findEntityByIdUsers(id);
			
			// Map the Users entity to UserDTO using ModelMapper
			UserDTO userDTO = modelMapper.map(user, UserDTO.class);
			
			// Wrap the UserDTO in a BaseDTO
			BaseDTO<UserDTO> response = new BaseDTO<>(HttpStatus.OK.value(), "User retrieved successfully", userDTO);
			
			return new ResponseEntity<>(response, HttpStatus.OK);
		} catch (ResourceNotFoundException e) {
			throw e;
		} catch (Exception e) {
			throw new BadRequestException("Failed to retrieve user: " + e.getMessage());
		}
	}
	
	/**
     * Retrieves a user by their username
     * @param username The username of the user to retrieve
     */
	@Override
    @Transactional(readOnly = true)
    public ResponseEntity<BaseDTO<UserDTO>> getUserByUsername(String username) {
		try {
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found with username: " + username));
            
            UserDTO userDTO = modelMapper.map(user, UserDTO.class);
            
            BaseDTO<UserDTO> response = new BaseDTO<>(HttpStatus.OK.value(), 
                    "User retrieved successfully", userDTO);
            
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new BadRequestException("Failed to retrieve user: " + e.getMessage());
        }
	}
	
	/**
     * Retrieves all users from the system 
     */
	@Override
    @Transactional(readOnly = true)
    public ResponseEntity<BaseDTO<List<UserDTO>>> getAllUsers() {
        try {
            List<UserDTO> userDTOs = userRepository.findAll().stream()
                    .map(user -> modelMapper.map(user, UserDTO.class))
                    .collect(Collectors.toList());
            
            BaseDTO<List<UserDTO>> response = new BaseDTO<>(HttpStatus.OK.value(), 
                    "Users retrieved successfully", userDTOs);
            
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            throw new BadRequestException("Failed to retrieve users: " + e.getMessage());
        }
    }
	
	/**
     * Creates a new user in the system 
     */
	@Override
	public ResponseEntity<BaseDTO<UserDTO>> createUser(UserDTO userDTO) {
        try {
            if (userRepository.existsByUsername(userDTO.getUsername())) {
                throw new BadRequestException("Username already exists: " + userDTO.getUsername());
            }
            
            User user = modelMapper.map(userDTO, User.class);
            User savedUser = userRepository.save(user);
            UserDTO savedUserDTO = modelMapper.map(savedUser, UserDTO.class);
            
            BaseDTO<UserDTO> response = new BaseDTO<>(HttpStatus.CREATED.value(), 
                    "User created successfully", savedUserDTO);
            
            return new ResponseEntity<>(response, HttpStatus.CREATED);
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            throw new BadRequestException("Failed to create user: " + e.getMessage());
        }
    }
	
	/**
     * Updates an existing user's information
     * Only the fields that are present in the userDTO will be updated
     */
	@Override
    public ResponseEntity<BaseDTO<UserDTO>> updateUser(Long id, UserDTO userDTO) {
        try {
            User existingUser = findEntityByIdUsers(id);
            
            // Check for username uniqueness only if username is being changed
            if (userDTO.getUsername() != null && 
                !existingUser.getUsername().equals(userDTO.getUsername()) && 
                userRepository.existsByUsername(userDTO.getUsername())) {
                throw new BadRequestException("Username already exists: " + userDTO.getUsername());
            }
            
            // Only update fields that are present in the DTO
            if (userDTO.getUsername() != null) {
                existingUser.setUsername(userDTO.getUsername());
            }
            if (userDTO.getFullName() != null) {
                existingUser.setFullName(userDTO.getFullName());
            }
            if (userDTO.getAvatarUrl() != null) {
                existingUser.setAvatarUrl(userDTO.getAvatarUrl());
            }
            
            User updatedUser = userRepository.save(existingUser);
            UserDTO updatedUserDTO = modelMapper.map(updatedUser, UserDTO.class);
            
            BaseDTO<UserDTO> response = new BaseDTO<>(HttpStatus.OK.value(), 
                    "User updated successfully", updatedUserDTO);
            
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new BadRequestException("Failed to update user: " + e.getMessage());
        }
    }
	
	/**
     * Deletes a user from the system by their ID
     * @param id The ID of the user to delete
     */
	@Override
	public ResponseEntity<BaseDTO<Void>> deleteUser(Long id) {
		try {
			User user = findEntityByIdUsers(id);
			userRepository.delete(user);
			
			BaseDTO<Void> response = new BaseDTO<>(HttpStatus.OK.value(), 
                    "User deleted successfully", null);
			return new ResponseEntity<>(response, HttpStatus.OK);
		} catch (ResourceNotFoundException e) {
			throw e;
		} catch (Exception e) {
			throw new BadRequestException("Failed to delete user: " + e.getMessage());
		}
	}
	
	/**
     * Checks if a user with the given username exists in the system
     * @param username The username to check
     */
	@Override
    @Transactional(readOnly = true)
    public boolean existsByUsername(String username) {
        try {
            return userRepository.existsByUsername(username);
        } catch (Exception e) {
            throw new BadRequestException("Failed to check username existence: " + e.getMessage());
        }
    }

	/**
     * Finds a user entity by ID
     * This is a helper method used internally by other service methods
     * @param id The ID of the user to find
     */
	@Override
	@Transactional(readOnly = true)
	public User findEntityByIdUsers(Long id) {
		try {
			return userRepository.findById(id)
					.orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
		} catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new BadRequestException("Failed to find user: " + e.getMessage());
        }
	}
	
    /**
     * Loads a user by username for Spring Security authentication.
     * This method is required by the UserDetailsService interface.
     * @param username The username to look up
     */
	@Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) {        
        // Find user in database
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    return new UsernameNotFoundException("User not found with username: " + username);
                });
        
        return buildUserDetails(user);
    }
	
	/**
     * Converts a User entity to a Spring Security UserDetails object.
     * @param user The user entity
     * @return UserDetails object with authentication information
     */
    private UserDetails buildUserDetails(User user) {
        // Convert user roles to Spring Security authorities
        Collection<GrantedAuthority> authorities = new ArrayList<>();
        
        // Default role if none assigned
        authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
        
        // Create Spring Security UserDetails object
        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),          // username
                user.getPassword(),          // password
                user.isActive(),             // enabled
                true,                        // accountNonExpired
                true,                        // credentialsNonExpired
                !user.isLocked(),            // accountNonLocked
                authorities                  // authorities/roles
        );
    }
	
}
