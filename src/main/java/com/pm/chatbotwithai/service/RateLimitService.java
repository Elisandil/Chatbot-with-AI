package com.pm.chatbotwithai.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
public class RateLimitService {
    private static final Logger logger = LoggerFactory.getLogger(RateLimitService.class);

    @Value("${chatbot.rate-limit.requests-per-minute:20}")
    private int requestsPerMinute;

    @Value("${chatbot.rate-limit.requests-per-hour:100}")
    private int requestsPerHour;

    @Value("${chatbot.rate-limit.burst-limit:5}")
    private int burstLimit;

    private final ConcurrentHashMap<String, UserRateLimitInfo> userLimits = new ConcurrentHashMap<>();
    private final ScheduledExecutorService cleanupExecutor = Executors.newSingleThreadScheduledExecutor();

    public RateLimitService() {
        cleanupExecutor.scheduleAtFixedRate(this::cleanupExpiredEntries, 5, 5, TimeUnit.MINUTES);
    }

    public boolean isAllowed(String userId) {

        if (userId == null || userId.trim().isEmpty()) {
            logger.warn("Rate limit check attempted with null/empty userId");
            return false;
        }
        UserRateLimitInfo userInfo = userLimits.computeIfAbsent(userId, k -> new UserRateLimitInfo());

        synchronized (userInfo) {
            LocalDateTime now = LocalDateTime.now();
            userInfo.cleanOldRequests(now);
            long recentRequests = userInfo.getRequestsInLast(now, 10, ChronoUnit.SECONDS);

            if (recentRequests >= burstLimit) {
                logger.warn("Burst limit exceeded for user {}: {} requests in 10 seconds",
                        userId, recentRequests);
                return false;
            }
            long requestsThisMinute = userInfo.getRequestsInLast(now, 1, ChronoUnit.MINUTES);

            if (requestsThisMinute >= requestsPerMinute) {
                logger.warn("Per-minute limit exceeded for user {}: {} requests this minute",
                        userId, requestsThisMinute);
                return false;
            }
            long requestsThisHour = userInfo.getRequestsInLast(now, 1, ChronoUnit.HOURS);

            if (requestsThisHour >= requestsPerHour) {
                logger.warn("Per-hour limit exceeded for user {}: {} requests this hour",
                        userId, requestsThisHour);
                return false;
            }
            userInfo.addRequest(now);

            logger.debug("Rate limit check passed for user {}: {}/min, {}/hour",
                    userId, requestsThisMinute + 1, requestsThisHour + 1);

            return true;
        }
    }

    public RateLimitStatus getRateLimitStatus(String userId) {

        if (userId == null || userId.trim().isEmpty()) {
            return new RateLimitStatus(0,
                    0,
                    0,
                    requestsPerMinute,
                    requestsPerHour,
                    burstLimit);
        }
        UserRateLimitInfo userInfo = userLimits.get(userId);

        if (userInfo == null) {
            return new RateLimitStatus(0,
                    0,
                    0,
                    requestsPerMinute,
                    requestsPerHour,
                    burstLimit);
        }
        synchronized (userInfo) {
            LocalDateTime now = LocalDateTime.now();
            userInfo.cleanOldRequests(now);
            long requestsThisMinute = userInfo.getRequestsInLast(now, 1, ChronoUnit.MINUTES);
            long requestsThisHour = userInfo.getRequestsInLast(now, 1, ChronoUnit.HOURS);
            long recentRequests = userInfo.getRequestsInLast(now, 10, ChronoUnit.SECONDS);

            return new RateLimitStatus(
                    (int) requestsThisMinute,
                    (int) requestsThisHour,
                    (int) recentRequests,
                    requestsPerMinute,
                    requestsPerHour,
                    burstLimit
            );
        }
    }

    public void resetUserLimits(String userId) {
        userLimits.remove(userId);
        logger.info("Rate limits reset for user: {}", userId);
    }

    /*
    public long getTimeUntilNextRequest(String userId) {
        UserRateLimitInfo userInfo = userLimits.get(userId);

        if (userInfo == null) {
            return 0;
        }
        synchronized (userInfo) {
            LocalDateTime now = LocalDateTime.now();
            userInfo.cleanOldRequests(now);
            long requestsThisMinute = userInfo.getRequestsInLast(now, 1, ChronoUnit.MINUTES);

            if (requestsThisMinute >= requestsPerMinute) {
                LocalDateTime oldestInMinute = userInfo.getOldestRequestInLast(now, 1, ChronoUnit.MINUTES);

                if (oldestInMinute != null) {
                    return ChronoUnit.SECONDS.between(now, oldestInMinute.plusMinutes(1));
                }
            }
            long recentRequests = userInfo.getRequestsInLast(now, 10, ChronoUnit.SECONDS);

            if (recentRequests >= burstLimit) {
                LocalDateTime oldestInBurst = userInfo.getOldestRequestInLast(now, 10, ChronoUnit.SECONDS);

                if (oldestInBurst != null) {
                    return ChronoUnit.SECONDS.between(now, oldestInBurst.plusSeconds(10));
                }
            }
            return 0;
        }
    }
    */


    private void cleanupExpiredEntries() {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(2);

        userLimits.entrySet().removeIf(entry -> {
            UserRateLimitInfo userInfo = entry.getValue();
            synchronized (userInfo) {
                userInfo.cleanOldRequests(LocalDateTime.now());
                return userInfo.isEmpty() || userInfo.getLastRequestTime().isBefore(cutoff);
            }
        });
        logger.debug("Cleaned up expired rate limit entries. Active users: {}", userLimits.size());
    }

    // Inner classes
    public static class RateLimitStatus {
        private final int currentMinuteRequests;
        private final int currentHourRequests;
        private final int currentBurstRequests;
        private final int maxMinuteRequests;
        private final int maxHourRequests;
        private final int maxBurstRequests;

        public RateLimitStatus(int currentMinuteRequests, int currentHourRequests,
                               int currentBurstRequests, int maxMinuteRequests,
                               int maxHourRequests, int maxBurstRequests) {
            this.currentMinuteRequests = currentMinuteRequests;
            this.currentHourRequests = currentHourRequests;
            this.currentBurstRequests = currentBurstRequests;
            this.maxMinuteRequests = maxMinuteRequests;
            this.maxHourRequests = maxHourRequests;
            this.maxBurstRequests = maxBurstRequests;
        }

        public int getCurrentMinuteRequests() { return currentMinuteRequests; }
        public int getCurrentHourRequests() { return currentHourRequests; }
        public int getCurrentBurstRequests() { return currentBurstRequests; }
        public int getMaxMinuteRequests() { return maxMinuteRequests; }
        public int getMaxHourRequests() { return maxHourRequests; }
        public int getMaxBurstRequests() { return maxBurstRequests; }

        public int getRemainingMinuteRequests() {
            return Math.max(0, maxMinuteRequests - currentMinuteRequests);
        }

        public int getRemainingHourRequests() {
            return Math.max(0, maxHourRequests - currentHourRequests);
        }

        public boolean isBlocked() {
            return currentMinuteRequests >= maxMinuteRequests ||
                    currentHourRequests >= maxHourRequests ||
                    currentBurstRequests >= maxBurstRequests;
        }
    }

    private static class UserRateLimitInfo {
        private final java.util.List<LocalDateTime> requestTimes = new java.util.ArrayList<>();

        public synchronized void addRequest(LocalDateTime timestamp) {
            requestTimes.add(timestamp);
        }

        public synchronized void cleanOldRequests(LocalDateTime now) {
            LocalDateTime cutoff = now.minusHours(1);
            requestTimes.removeIf(timestamp -> timestamp.isBefore(cutoff));
        }

        public synchronized long getRequestsInLast(LocalDateTime now, long amount, ChronoUnit unit) {
            LocalDateTime cutoff = now.minus(amount, unit);
            return requestTimes.stream()
                    .mapToLong(timestamp -> timestamp.isAfter(cutoff) ? 1 : 0)
                    .sum();
        }

        public synchronized LocalDateTime getOldestRequestInLast(LocalDateTime now, long amount, ChronoUnit unit) {
            LocalDateTime cutoff = now.minus(amount, unit);
            return requestTimes.stream()
                    .filter(timestamp -> timestamp.isAfter(cutoff))
                    .min(LocalDateTime::compareTo)
                    .orElse(null);
        }

        public synchronized boolean isEmpty() {
            return requestTimes.isEmpty();
        }

        public synchronized LocalDateTime getLastRequestTime() {
            return requestTimes.isEmpty() ?
                    LocalDateTime.now().minusHours(2) :
                    requestTimes.get(requestTimes.size() - 1);
        }
    }
}
