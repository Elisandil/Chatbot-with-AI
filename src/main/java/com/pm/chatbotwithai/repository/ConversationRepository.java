package com.pm.chatbotwithai.repository;

import com.pm.chatbotwithai.model.entity.Conversation;
import com.pm.chatbotwithai.model.entity.ConversationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, UUID> {

    Page<Conversation> findByUserIdAndStatusOrderByLastActivityDesc(
            String userId,
            ConversationStatus status,
            Pageable pageable
    );

    List<Conversation> findByUserIdAndStatusOrderByLastActivityDesc(
            String userId,
            ConversationStatus status
    );

    Optional<Conversation> findByIdAndUserId(UUID id, String userId);

    @Query("SELECT COUNT(c) FROM Conversation c WHERE c.userId = :userId AND c.status = :status")
    long countByUserIdAndStatus(@Param("userId") String userId,
                                @Param("status") ConversationStatus status);

    @Query("SELECT c FROM Conversation c WHERE c.lastActivity < :cutoffDate AND c.status = :status")
    List<Conversation> findInactiveConversations(@Param("cutoffDate") LocalDateTime cutoffDate,
                                                 @Param("status") ConversationStatus status);

    void deleteByUserIdAndStatus(String userId, ConversationStatus status);
}
