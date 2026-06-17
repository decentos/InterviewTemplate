package interview.messages.queue;

import interview.messages.model.RedisMessage;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class RedisMessageQueue implements MessageQueue {

    private static final String QUEUE_KEY_PREFIX = "llm:messages:";

    private final StringRedisTemplate redisTemplate;

    public RedisMessageQueue(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void enqueue(RedisMessage message) {
        redisTemplate.opsForList().rightPush(queueKey(message.getUserId()), message.getMessageId());
    }

    @Override
    public List<String> messageIds(String userId) {
        return Optional.ofNullable(redisTemplate.opsForList().range(queueKey(userId), 0, -1))
                .orElse(List.of());
    }

    @Override
    public boolean isEmpty(String userId) {
        return size(userId) == 0;
    }

    @Override
    public long size(String userId) {
        Long size = redisTemplate.opsForList().size(queueKey(userId));
        return size == null ? 0 : size;
    }

    static String queueKey(String userId) {
        return QUEUE_KEY_PREFIX + userId;
    }
}
