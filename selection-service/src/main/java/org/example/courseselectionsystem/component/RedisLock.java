package org.example.courseselectionsystem.component;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.UUID;

/**
 * 基于 Redis SETNX 的分布式锁（选课/退课防超卖）。
 * - 加锁键：lock:course:{courseId} / lock:selection:{selectionId}
 * - 通过 value=UUID 保证只有持锁方才能释放（防误删他人锁）
 * - 自动过期兜底：即使程序异常未释放，锁也会在 TTL 后自动失效
 * - Redis 不可用时优雅降级：降级为“无锁执行”，不阻断业务
 */
@Component
public class RedisLock {

    private static final Logger log = LoggerFactory.getLogger(RedisLock.class);

    private static final String COURSE_PREFIX = "lock:course:";
    private static final String SELECTION_PREFIX = "lock:selection:";
    private static final Duration LOCK_TTL = Duration.ofSeconds(15);

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 尝试加课程锁（非阻塞）
     *
     * @return true=拿到锁（或 Redis 不可用已降级）；false=已被其它请求持有
     */
    public boolean tryLockCourse(Long courseId, String owner) {
        if (courseId == null) {
            return true;
        }
        return trySet(COURSE_PREFIX + courseId, owner);
    }

    /**
     * 尝试加选课记录锁（退课时串行化候补晋升）
     */
    public boolean tryLockSelection(Long selectionId, String owner) {
        if (selectionId == null) {
            return true;
        }
        return trySet(SELECTION_PREFIX + selectionId, owner);
    }

    private boolean trySet(String key, String owner) {
        try {
            return Boolean.TRUE.equals(stringRedisTemplate.opsForValue()
                    .setIfAbsent(key, owner, LOCK_TTL));
        } catch (Exception e) {
            log.warn("Redis 加锁不可用，降级为无锁执行 key={}: {}", key, e.getMessage());
            return true;
        }
    }

    /**
     * 带短重试的课程锁：抢选高峰时轻微等待（最多约 1.5 秒），避免并发请求直接被拒
     */
    public boolean tryLockCourseWithRetry(Long courseId, String owner, int attempts) {
        int tryTimes = Math.max(1, attempts);
        for (int i = 0; i < tryTimes; i++) {
            if (tryLockCourse(courseId, owner)) {
                return true;
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return false;
    }

    public String newOwner() {
        return UUID.randomUUID().toString();
    }

    public void unlockCourse(Long courseId, String owner) {
        unlockKey(COURSE_PREFIX + courseId, owner);
    }

    public void unlockSelection(Long selectionId, String owner) {
        unlockKey(SELECTION_PREFIX + selectionId, owner);
    }

    private void unlockKey(String key, String owner) {
        try {
            String current = stringRedisTemplate.opsForValue().get(key);
            if (current != null && current.equals(owner)) {
                stringRedisTemplate.delete(key);
            }
        } catch (Exception e) {
            log.warn("Redis 释放锁异常 key={}: {}", key, e.getMessage());
        }
    }
}
