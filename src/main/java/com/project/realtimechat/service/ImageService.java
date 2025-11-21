package com.project.realtimechat.service;

import java.util.List;
import java.util.Optional;

import org.springframework.web.multipart.MultipartFile;

import com.project.realtimechat.entity.ImageDocument;

public interface ImageService {
	ImageDocument saveImage(MultipartFile file, String uploadedBy, Long chatRoomId, Long messageId);
    ImageDocument updateImage(ImageDocument imageDocument);
    Optional<ImageDocument> getImageById(String id);
    List<ImageDocument> getImagesByChatRoom(Long chatRoomId);
    List<ImageDocument> getImagesByUser(String uploadedBy);
    void deleteImage(String id);
    void deleteImageByMessageId(Long messageId);
    String generateImageUrl(String imageId);
}
