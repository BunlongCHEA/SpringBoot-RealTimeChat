package com.project.realtimechat.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.project.realtimechat.entity.ImageDocument;

@Repository
public interface ImageRepository extends MongoRepository<ImageDocument, String> {
	List<ImageDocument> findByChatRoomId(Long chatRoomId);
	List<ImageDocument> findByUploadedBy(String uploadedBy);
	Optional<ImageDocument> findByMessageId(Long messageId);
	void deleteByMessageId(Long messageId);
}
