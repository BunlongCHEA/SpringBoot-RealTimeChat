package com.project.realtimechat.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.core.MongoTemplate;

@Configuration
public class MongoConfig {
    @Bean
    public MongoTemplate mongoTemplate(MongoDatabaseFactory mongoDbFactory) {
        return new MongoTemplate(mongoDbFactory);
    }
    
    @Bean
    public CommandLineRunner initDatabase(MongoTemplate mongoTemplate) {
        return args -> {
            try {
                // Test connection and create database if not exists
                mongoTemplate.getDb().listCollectionNames();
                System.out.println("✅ MongoDB connection successful!");
                System.out.println("📦 Database: " + mongoTemplate.getDb().getName());
            } catch (Exception e) {
                System.err.println("❌ MongoDB connection failed: " + e.getMessage());
            }
        };
    }
}
