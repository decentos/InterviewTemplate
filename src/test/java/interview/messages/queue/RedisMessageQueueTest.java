package interview.messages.queue;

import interview.messages.MessageStatus;
import interview.messages.model.RedisMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.lang.reflect.Proxy;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;

class RedisMessageQueueTest {

    private RedisMessageQueue messageQueue;

    @BeforeEach
    void setUp() {
        messageQueue = new RedisMessageQueue(new FakeRedisTemplate());
    }

    @Test
    void shouldKeepMessageIdsInFifoOrderWithoutRemovingThem() {
        var firstMessage = message("message-1", "user-1");
        var secondMessage = message("message-2", "user-1");

        messageQueue.enqueue(firstMessage);
        messageQueue.enqueue(secondMessage);

        assertAll(
                () -> assertEquals(2, messageQueue.size("user-1")),
                () -> assertFalse(messageQueue.isEmpty("user-1")),
                () -> assertIterableEquals(List.of("message-1", "message-2"), messageQueue.messageIds("user-1")),
                () -> assertIterableEquals(List.of("message-1", "message-2"), messageQueue.messageIds("user-1")),
                () -> assertEquals(2, messageQueue.size("user-1"))
        );
    }

    @Test
    void shouldKeepSeparateQueuesPerUser() {
        var firstUserMessage = message("message-1", "user-1");
        var secondUserMessage = message("message-2", "user-2");

        messageQueue.enqueue(firstUserMessage);
        messageQueue.enqueue(secondUserMessage);

        assertAll(
                () -> assertEquals(1, messageQueue.size("user-1")),
                () -> assertEquals(1, messageQueue.size("user-2")),
                () -> assertIterableEquals(List.of(secondUserMessage.getMessageId()), messageQueue.messageIds("user-2")),
                () -> assertIterableEquals(List.of(firstUserMessage.getMessageId()), messageQueue.messageIds("user-1"))
        );
    }

    private RedisMessage message(String messageId, String userId) {
        return new RedisMessage(messageId, userId, "question", MessageStatus.Queued, null);
    }

    private static class FakeRedisTemplate extends StringRedisTemplate {

        private final Map<String, Deque<String>> queues = new HashMap<>();

        @Override
        @SuppressWarnings("unchecked")
        public ListOperations<String, String> opsForList() {
            return (ListOperations<String, String>) Proxy.newProxyInstance(
                    ListOperations.class.getClassLoader(),
                    new Class<?>[]{ListOperations.class},
                    (proxy, method, args) -> switch (method.getName()) {
                        case "rightPush" -> rightPush((String) args[0], (String) args[1]);
                        case "range" -> range((String) args[0], (Long) args[1], (Long) args[2]);
                        case "size" -> size((String) args[0]);
                        default -> throw new UnsupportedOperationException(method.getName());
                    }
            );
        }

        private Long rightPush(String key, String messageId) {
            Deque<String> queue = queues.computeIfAbsent(key, ignored -> new ArrayDeque<>());
            queue.addLast(messageId);
            return (long) queue.size();
        }

        private List<String> range(String key, Long start, Long end) {
            Deque<String> queue = queues.get(key);
            if (queue == null) {
                return List.of();
            }

            return List.copyOf(queue);
        }

        private Long size(String key) {
            Deque<String> queue = queues.get(key);
            return queue == null ? 0 : (long) queue.size();
        }
    }
}
