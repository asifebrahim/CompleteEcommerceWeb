package com.example.EcommerceFresh.Service;

import com.example.EcommerceFresh.Dao.PendingUserDao;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class PendingUserCleanupService {

    private final PendingUserDao pendingUserDao;

    public PendingUserCleanupService(PendingUserDao pendingUserDao) {
        this.pendingUserDao = pendingUserDao;
    }

    @Scheduled(fixedRateString = "${app.pending.cleanupMs:1800000}") // default every 30 minutes
    public void cleanupExpired() {
        try {
            pendingUserDao.deleteByOtpExpiryBefore(LocalDateTime.now());
        } catch (Exception e) {
            // log error if logging is configured; for now print stacktrace
            e.printStackTrace();
        }
    }
}
