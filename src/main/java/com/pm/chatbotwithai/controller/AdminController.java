package com.pm.chatbotwithai.controller;

import com.pm.chatbotwithai.service.ConversationService;
import com.pm.chatbotwithai.service.RateLimitService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {
    private static final Logger logger = LoggerFactory.getLogger(AdminController.class);

    private final ConversationService conversationService;
    private final RateLimitService rateLimitService;

    @Autowired
    public AdminController(ConversationService conversationService, RateLimitService rateLimitService) {
        this.conversationService = conversationService;
        this.rateLimitService = rateLimitService;
    }

    @DeleteMapping("/rate-limit/{userId}")
    public ResponseEntity<Void> resetUserRateLimit(@PathVariable String userId) {
        logger.info("Admin resetting rate limits for user: {}", userId);
        rateLimitService.resetUserLimits(userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/cleanup/conversations")
    public ResponseEntity<Void> cleanupOldConversations(
            @RequestParam(defaultValue = "30") int daysOld) {

        logger.info("Admin triggering cleanup of conversations older than {} days", daysOld);
        conversationService.deleteOldConversations(daysOld);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/stats/system")
    public ResponseEntity<SystemStats> getSystemStats() {
        // This is a placeholder for demo purposes
        return ResponseEntity.ok(new SystemStats());
    }

    private static class SystemStats {
        private final long totalConversations = 0;
        private final long totalMessages = 0;
        private final long activeUsers = 0;

        public long getTotalConversations() { return totalConversations; }
        public long getTotalMessages() { return totalMessages; }
        public long getActiveUsers() { return activeUsers; }
    }
}
