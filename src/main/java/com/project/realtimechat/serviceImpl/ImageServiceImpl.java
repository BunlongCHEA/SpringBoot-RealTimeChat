package com.project.realtimechat.serviceImpl;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.project.realtimechat.entity.ImageDocument;
import com.project.realtimechat.exception.BadRequestException;
import com.project.realtimechat.exception.ResourceNotFoundException;
import com.project.realtimechat.repository.ImageRepository;
import com.project.realtimechat.service.ImageService;

import io.jsonwebtoken.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor // Lombok dependency injection that generates a constructor for all final, but more Immutability, Compile-Time Safety, Boilerplate Reduction than Autowired 
@Slf4j  // Provide simple logging for log 
public class ImageServiceImpl implements ImageService {
	private final ImageRepository imageRepository;
	
	@Value("${app.base-url:http://localhost:8080}")
	private String baseUrl;
	
	private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
    private static final String[] ALLOWED_CONTENT_TYPES = {
        "image/jpeg", "image/jpg", "image/png", "image/gif", "image/webp"
    };
	
    @Override
	public ImageDocument saveImage(MultipartFile file, String uploadedBy, Long chatRoomId, Long messageId) {
		try {
			validateImage(file);
			
			ImageDocument imageDocument = new ImageDocument();
//            imageDocument.setId(UUID.randomUUID().toString());
            imageDocument.setFilename(file.getOriginalFilename());
            imageDocument.setContentType(file.getContentType());
            imageDocument.setSize(file.getSize());
            try {
				imageDocument.setData(file.getBytes());
			} catch (java.io.IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
            imageDocument.setUploadedBy(uploadedBy);
            imageDocument.setUploadedAt(Instant.now());
            imageDocument.setChatRoomId(chatRoomId);
            imageDocument.setMessageId(messageId);
			
            ImageDocument savedImage = imageRepository.save(imageDocument);
            log.info("[{}] | Image saved successfully with ID: {}", Instant.now(), savedImage.getId());
            
            return savedImage;
		} catch (IOException e) {
			log.error("[{}] | Error saving image: {}", Instant.now(), e.getMessage());
            throw new BadRequestException("Failed to save image: " + e.getMessage());
		}
	}

    @Override
    public ImageDocument updateImage(ImageDocument imageDocument) {
        try {
            ImageDocument updated = imageRepository.save(imageDocument);
            log.info("Updated image document with ID: {}", updated.getId());
            return updated;
        } catch (Exception e) {
            log.error("Failed to update image document: {}", e.getMessage());
            throw new RuntimeException("Failed to update image: " + e.getMessage());
        }
    }
    
    @Override
    public Optional<ImageDocument> getImageById(String id) {
        return imageRepository.findById(id);
    }
    
    @Override
    public List<ImageDocument> getImagesByChatRoom(Long chatRoomId) {
        return imageRepository.findByChatRoomId(chatRoomId);
    }

    @Override
    public List<ImageDocument> getImagesByUser(String uploadedBy) {
        return imageRepository.findByUploadedBy(uploadedBy);
    }
	
    @Override
    public void deleteImage(String id) {
        if (!imageRepository.existsById(id)) {
            throw new ResourceNotFoundException("Image not found with ID: " + id);
        }
        imageRepository.deleteById(id);
        log.info("Image deleted successfully with ID: {}", id);
    }

    @Override
    public void deleteImageByMessageId(Long messageId) {
        imageRepository.deleteByMessageId(messageId);
        log.info("Images deleted for message ID: {}", messageId);
    }
    
//	--- Helper function ---
	
	@Override
    public String generateImageUrl(String imageId) {
        // return baseUrl + "/api/images/" + imageId;
        // Ensure baseUrl doesn't end with slash and path starts with slash
        String cleanBaseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        String imageUrl = cleanBaseUrl + "/api/v1/images/" + imageId;
        
        log.info("[{}] | Generated image URL: {}", Instant.now(), imageUrl);
        return imageUrl;
    }
	
	/**
	 * Allow validate the image with at least 1 image, content-type, and file-size
	 */
	private void validateImage(MultipartFile file) {
        if (file.isEmpty()) {
            throw new BadRequestException("File is empty");
        }
        
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BadRequestException("File size exceeds maximum limit of 10MB");
        }
        
        String contentType = file.getContentType();
        if (contentType == null || !isValidContentType(contentType)) {
            throw new BadRequestException("Invalid file type. Only JPEG, PNG, GIF, and WebP images are allowed");
        }
    }
	
	private boolean isValidContentType(String contentType) {
        for (String allowedType : ALLOWED_CONTENT_TYPES) {
            if (allowedType.equals(contentType)) {
                return true;
            }
        }
        return false;
    }
}
