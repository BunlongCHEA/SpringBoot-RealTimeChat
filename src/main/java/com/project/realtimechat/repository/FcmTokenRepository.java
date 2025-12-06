package com.project.realtimechat.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.project.realtimechat.entity.FcmToken;
import com.project.realtimechat.entity.User;

@Repository
public interface FcmTokenRepository extends JpaRepository<FcmToken, Long> {
    List<FcmToken> findByUserIdAndIsActiveTrue(Long userId);

    List<FcmToken> findByUserAndIsActiveTrue(User user);

    Optional<FcmToken> findByToken(String token);

    Optional<FcmToken> findByUserIdAndToken(Long userId, String token);

    @Query("SELECT f FROM FcmToken f WHERE f.user.id IN :userIds AND f.isActive = true")
    List<FcmToken> findActiveTokensByUserIds(@Param("userIds") List<Long> userIds);

    @Modifying
    @Transactional
    @Query("UPDATE FcmToken f SET f.isActive = false WHERE f. token = :token")
    void deactivateToken(@Param("token") String token);

    @Modifying
    @Transactional
    @Query("DELETE FROM FcmToken f WHERE f.user.id = :userId AND f.token = :token")
    void deleteByUserIdAndToken(@Param("userId") Long userId, @Param("token") String token);
}