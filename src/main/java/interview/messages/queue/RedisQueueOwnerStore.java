package interview.messages.queue;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
class RedisQueueOwnerStore implements QueueOwnerStore {

    private static final DefaultRedisScript<Long> RELEASE_IF_OWNER_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) end return 0",
            Long.class
    );

    private final StringRedisTemplate redisTemplate;

    RedisQueueOwnerStore(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public boolean setOwnerIfAbsent(String key, String ownerId) {
        return Boolean.TRUE.equals(redisTemplate.opsForValue().setIfAbsent(key, ownerId));
    }

    @Override
    public String findOwner(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    @Override
    public void releaseIfOwner(String key, String ownerId) {
        redisTemplate.execute(RELEASE_IF_OWNER_SCRIPT, List.of(key), ownerId);
    }
}
