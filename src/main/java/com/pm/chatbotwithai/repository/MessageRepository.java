package com.pm.chatbotwithai.repository;

import com.pm.chatbotwithai.model.entity.Message;
import com.pm.chatbotwithai.model.entity.SenderType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface MessageRepository extends JpaRepository<Message, UUID> {

    Page<Message> findByConversationIdOrderByCreatedAtAsc(UUID conversationId, Pageable pageable);

    List<Message> findByConversationIdOrderByCreatedAtAsc(UUID conversationId);

    @Query("SELECT m FROM Message m WHERE m.conversation.id = :conversationId " +
            "ORDER BY m.createdAt DESC")
    List<Message> findLatestMessagesByConversationId(@Param("conversationId") UUID conversationId,
                                                     Pageable pageable);

    @Query("SELECT COUNT(m) FROM Message m WHERE m.conversation.userId = :userId " +
            "AND m.createdAt > :since")
    long countUserMessagesAfter(@Param("userId") String userId,
                                @Param("since") LocalDateTime since);

    @Query("SELECT AVG(m.processingTimeMs) FROM Message m WHERE m.senderType = 'AI' " +
            "AND m.createdAt > :since")
    Double getAverageProcessingTime(@Param("since") LocalDateTime since);

    @Query("SELECT m FROM Message m WHERE m.conversation.userId = :userId " +
            "AND m.senderType = :senderType " +
            "ORDER BY m.createdAt DESC")
    List<Message> findUserMessagesBySenderType(@Param("userId") String userId,
                                               @Param("senderType") SenderType senderType,
                                               Pageable pageable);
}
