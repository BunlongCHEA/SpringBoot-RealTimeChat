package com.project.realtimechat.config;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import lombok.extern.slf4j.Slf4j;

@Configuration
@Slf4j
public class FirebaseConfig {
    // @Value("${firebase.config.path:firebase-service-account.json}")
    // private String firebaseConfigPath;

    @Value("${firebase.credentials.encoded:}")
    private String firebaseCredentialsEncoded;

    // @Value("${firebase.project.id:}")
    // private String firebaseProjectId;

    @Value("${firebase.enabled:true}")
    private boolean firebaseEnabled;

    // Decoded service account fields
    private String projectId;
    private String clientEmail;
    private String privateKeyId;
    private String privateKey;
    private String authUri;
    private String tokenUri;

    @PostConstruct
    public void initialize() {
        if (!firebaseEnabled) {
            log.info("Firebase is disabled in configuration");
            return;
        }

        if (firebaseCredentialsEncoded == null || firebaseCredentialsEncoded.trim().isEmpty()) {
            log.warn("Firebase credentials not configured - push notifications will be disabled");
            return;
        }

        try {
            if (FirebaseApp.getApps().isEmpty()) {
                // InputStream serviceAccount = new ClassPathResource(firebaseConfigPath).getInputStream();
                
                // FirebaseOptions options = FirebaseOptions.builder()
                //         .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                //         .build();

                // FirebaseApp.initializeApp(options);

                // ---

                // byte[] decodedCredentials = Base64.getDecoder().decode(firebaseCredentialsEncoded);
                // InputStream credentialsStream = new ByteArrayInputStream(decodedCredentials);
                // FirebaseOptions.Builder optionsBuilder = FirebaseOptions.builder()
                        // .setCredentials(GoogleCredentials.fromStream(credentialsStream));
                //// Optionally set project ID if provided
                // if (firebaseProjectId != null && !firebaseProjectId.trim().isEmpty()) {
                //     optionsBuilder.setProjectId(firebaseProjectId);
                // }
                // FirebaseApp app = FirebaseApp.initializeApp(optionsBuilder.build());

                // ---

                // Step 1: Decode Base64 credentials
                byte[] decodedCredentials = Base64.getDecoder().decode(firebaseCredentialsEncoded);
                String jsonString = new String(decodedCredentials);
                log.info("📄 Decoded Firebase credentials JSON");

                // Step 2: Parse JSON to extract fields
                Gson gson = new Gson();
                JsonObject jsonObject = gson.fromJson(jsonString, JsonObject.class);

                 // Step 3: Extract individual fields
                this.projectId = jsonObject.get("project_id").getAsString();
                this.clientEmail = jsonObject.get("client_email").getAsString();
                this.privateKeyId = jsonObject.get("private_key_id").getAsString();
                this.privateKey = jsonObject.get("private_key").getAsString();
                this.authUri = jsonObject.get("auth_uri"). getAsString();
                this.tokenUri = jsonObject.get("token_uri").getAsString();

                log.info("Extracted Firebase credentials:");
                log.info("   - Project ID: {}", projectId);
                log.info("   - Client Email: {}", clientEmail);
                log.info("   - Private Key ID: {}... {}", 
                        privateKeyId.substring(0, Math.min(8, privateKeyId.length())),
                        privateKeyId.substring(Math.max(0, privateKeyId.length() - 8)));
                log.info("   - Private Key ID: {}...", privateKey);
                log.info("   - Auth URI: {}", authUri);
                log.info("   - Token URI: {}", tokenUri);

                // Step 4: Create credentials stream from decoded JSON
                InputStream credentialsStream = new ByteArrayInputStream(decodedCredentials);

                // Step 5: Build Firebase options
                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(credentialsStream))
                        .setProjectId(projectId) // Use extracted project ID
                        .build();

                // Step 6: Initialize Firebase app
                FirebaseApp app = FirebaseApp.initializeApp(options);
                
                // Verify initialization
                log.info("Firebase initialized successfully");
                log.info("   - Project ID: {}", app.getOptions().getProjectId());
                log.info("   - App Name: {}", app.getName());
                
                // Step 7: Verify Firebase Messaging
                try {
                    FirebaseMessaging messaging = FirebaseMessaging.getInstance(app);
                    log.info("Firebase Messaging instance created successfully");
                } catch (Exception e) {
                    log.error("Failed to create Firebase Messaging instance: {}", e.getMessage());
                }

                // log.info("Firebase initialized successfully");
            } else {
                log.info("FirebaseApp already initialized");
                FirebaseApp app = FirebaseApp.getInstance();
                this.projectId = app.getOptions().getProjectId();
                log.info("   - Existing Project ID: {}", projectId);
            }
        } catch (IllegalArgumentException e) {
            log.error("Invalid Base64 encoding for Firebase credentials: {}", e.getMessage());
        } catch (IOException e) {
            log.error("Failed to initialize Firebase: {}", e.getMessage(), e);
        }
    }

    @Bean
    public FirebaseMessaging firebaseMessaging() {
        if (!firebaseEnabled || FirebaseApp.getApps().isEmpty()) {
            log.warn("FirebaseMessaging bean not created - Firebase not initialized");
            return null;
        }

        // return FirebaseMessaging.getInstance();
        try {
            FirebaseMessaging messaging = FirebaseMessaging.getInstance();
            log.info("FirebaseMessaging bean created successfully");
            return messaging;
        } catch (Exception e) {
            log.error("Failed to create FirebaseMessaging bean: {}", e.getMessage(), e);
            return null;
        }
    }

    // Getter methods for extracted fields (optional, for use in other beans)
    public String getProjectId() {
        return projectId;
    }

    public String getClientEmail() {
        return clientEmail;
    }

    public String getPrivateKeyId() {
        return privateKeyId;
    }

    public String getPrivateKey() {
        return privateKey;
    }

    public String getAuthUri() {
        return authUri;
    }

    public String getTokenUri() {
        return tokenUri;
    }
}
