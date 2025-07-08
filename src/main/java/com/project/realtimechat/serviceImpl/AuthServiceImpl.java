package com.project.realtimechat.serviceImpl;

import java.time.Instant;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.project.realtimechat.config.JwtService;
import com.project.realtimechat.dto.BaseDTO;
import com.project.realtimechat.dto.LoginRequest;
import com.project.realtimechat.dto.LoginResponse;
import com.project.realtimechat.dto.UserDTO;
import com.project.realtimechat.entity.User;
import com.project.realtimechat.exception.BadRequestException;
import com.project.realtimechat.repository.UserRepository;
import com.project.realtimechat.service.AuthService;

@Service
public class AuthServiceImpl implements AuthService {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private ModelMapper modelMapper;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    private AuthenticationManager authenticationManager;
    
    @Autowired
    private JwtService jwtService;
    
    /**
     * Registers a new user with encoded password
     */
    @Override
    public ResponseEntity<BaseDTO<UserDTO>> registerUser(UserDTO userDTO) {
    	try {
            // Check if username already exists
            if (userRepository.existsByUsername(userDTO.getUsername())) {
                throw new BadRequestException("Username already exists: " + userDTO.getUsername());
            }
            
            // Create user entity and set properties
            User user = modelMapper.map(userDTO, User.class);
            
            // Set creation and update timestamps
            Instant now = Instant.now();
            user.setCreatedAt(now);
            user.setUpdatedAt(now);
            
            // Encode password before saving
            user.setPassword(passwordEncoder.encode(userDTO.getPassword()));
            
            // Save user to database
            User savedUser = userRepository.save(user);
            
            // Return user data without password
            UserDTO savedUserDTO = modelMapper.map(savedUser, UserDTO.class);
            savedUserDTO.setPassword(null); // Don't return password
            
            BaseDTO<UserDTO> response = new BaseDTO<>(
                    HttpStatus.CREATED.value(),
                    "User registered successfully",
                    savedUserDTO);
            
            return new ResponseEntity<>(response, HttpStatus.CREATED);
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            throw new BadRequestException("Failed to register user: " + e.getMessage());
        }
    }
    
    /**
     * Authenticates a user and generates JWT token
     */
    @Override
    public ResponseEntity<BaseDTO<LoginResponse>> authenticateUser(LoginRequest loginRequest) {
    	try {
            // Authenticate user
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getUsername(),
                            loginRequest.getPassword()
                    )
            );
            
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            
            // Generate JWT tokens
            String accessToken = jwtService.generateToken(userDetails);
            String refreshToken = jwtService.generateRefreshToken(userDetails);
            
            // Get user details
            User user = userRepository.findByUsername(loginRequest.getUsername())
                    .orElseThrow(() -> new BadRequestException("User not found"));
            
            // Update last login time
            user.setLastLogin(Instant.now());
            userRepository.save(user);
            
            // Create response with tokens and user info
            UserDTO userDTO = modelMapper.map(user, UserDTO.class);
            userDTO.setPassword(null); // Don't return password
            
            LoginResponse loginResponse = new LoginResponse(accessToken, refreshToken, userDTO);
            
            BaseDTO<LoginResponse> response = new BaseDTO<>(
                    HttpStatus.OK.value(),
                    "Login successful",
                    loginResponse);
            
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            throw new BadRequestException("Authentication failed: " + e.getMessage());
        }
    }
}