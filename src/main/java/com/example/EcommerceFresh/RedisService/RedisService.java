package com.example.EcommerceFresh.RedisService;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class RedisService {
    private RedisTemplate redisTemplate;
    public RedisService(RedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }
    public boolean isAllowed(String email,int maxRequests,Long timeWindow){
        System.out.println("This method is being called");
        String key="rate_limiter"+email;
        Long count=(Long) redisTemplate.opsForValue().increment(key);
        if(count==1){
            redisTemplate.expire(key, timeWindow, TimeUnit.SECONDS);
        }
        return count<=maxRequests;
    }
    public void resetRateLimit(String email) {
        String key = "rate_limiter" + email;
        redisTemplate.delete(key);
    }
}
