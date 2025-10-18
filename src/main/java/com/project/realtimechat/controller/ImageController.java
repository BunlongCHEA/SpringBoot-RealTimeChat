package com.project.realtimechat.controller;

import java.util.Optional;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.project.realtimechat.dto.BaseDTO;
import com.project.realtimechat.entity.ImageDocument;
import com.project.realtimechat.repository.ImageRepository;
import com.project.realtimechat.service.ImageService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/images")
@RequiredArgsConstructor
public class ImageController {
	private final ImageService imageService;
	
	@GetMapping("/{imageId}")
	public ResponseEntity<BaseDTO<byte[]>> getImage(@PathVariable Long imageId) {
		Optional<ImageDocument> imageDocument = imageService.getImageById(imageId);
		
		if (imageDocument.isPresent()) {
			ImageDocument image = imageDocument.get();
			
			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.parseMediaType(image.getContentType()));
            headers.setContentLength(image.getSize());
            headers.setContentDispositionFormData("inline", image.getFilename());
            
            BaseDTO<byte[]> response = new BaseDTO<>(
                    HttpStatus.OK.value(),
                    "Image retrieved successfully",
                    image.getData()
                );
            
            return new ResponseEntity<>(response, headers, HttpStatus.OK);
		} else {
			BaseDTO<byte[]> response = new BaseDTO<>(
	                HttpStatus.NOT_FOUND.value(),
	                "Image not found with ID: " + imageId,
	                null
	            );
			
			return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
		}		
	}
	
	@DeleteMapping("/{imageId}")
	public ResponseEntity<BaseDTO<Void>> deleteImage(@PathVariable Long imageId) {
		try {
            imageService.deleteImage(imageId);
            
            BaseDTO<Void> response = new BaseDTO<>(
                HttpStatus.NO_CONTENT.value(),
                "Image deleted successfully",
                null
            );
            
            return new ResponseEntity<>(response, HttpStatus.NO_CONTENT);
        } catch (Exception e) {
            BaseDTO<Void> response = new BaseDTO<>(
                HttpStatus.NOT_FOUND.value(),
                "Image not found with ID: " + imageId,
                null
            );
            return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
        }
	}
	
	
	
}
