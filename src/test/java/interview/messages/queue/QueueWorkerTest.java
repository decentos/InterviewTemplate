package interview.messages.queue;

import interview.llm.LlmClient;
import interview.messages.MessageStatus;
import interview.messages.model.RedisMessage;
import interview.messages.storage.MessageStore;
import interview.messages.worker.QueueWorker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QueueWorkerTest {

    private InMemoryMessageQueue messageQueue;
    private InMemoryMessageStore messageStore;
    private InMemoryQueueOwnerStore queueOwnerStore;
    private QueueOwnershipService queueOwnershipService;
    private RecordingLlmClient llmClient;
    private QueueWorker queueWorker;

    @BeforeEach
    void setUp() {
        messageQueue = new InMemoryMessageQueue();
        messageStore = new InMemoryMessageStore();
        queueOwnerStore = new InMemoryQueueOwnerStore();
        queueOwnershipService = new QueueOwnershipService(queueOwnerStore, "owner-1");
        llmClient = new RecordingLlmClient();
        queueWorker = new QueueWorker(messageQueue, messageStore, queueOwnershipService, llmClient);
    }

    @Test
    void shouldProcessUserQueueSequentially() {
        addQueuedMessage(message("message-1", "user-1", "question-1"));
        addQueuedMessage(message("message-2", "user-1", "question-2"));
        assertTrue(queueOwnershipService.tryAcquire("user-1"));

        queueWorker.processQueue("user-1");

        assertAll(
                () -> assertEquals(List.of("question-1", "question-2"), llmClient.questions),
                () -> assertEquals(List.of(MessageStatus.Processing, MessageStatus.Done), messageStore.statuses("message-1")),
                () -> assertEquals(List.of(MessageStatus.Processing, MessageStatus.Done), messageStore.statuses("message-2")),
                () -> assertEquals("answer for question-1", messageStore.findById("message-1").orElseThrow().getAnswer()),
                () -> assertEquals("answer for question-2", messageStore.findById("message-2").orElseThrow().getAnswer()),
                () -> assertEquals(2, messageQueue.size("user-1")),
                () -> assertFalse(queueOwnershipService.isOwner("user-1"))
        );
    }

    @Test
    void shouldContinueWhenMessageAppearsDuringRelease() {
        boolean[] messageAdded = {false};
        queueOwnerStore.onRelease = () -> {
            if (!messageAdded[0]) {
                messageAdded[0] = true;
                addQueuedMessage(message("message-1", "user-1", "question-1"));
            }
        };
        assertTrue(queueOwnershipService.tryAcquire("user-1"));

        queueWorker.processQueue("user-1");

        assertAll(
                () -> assertEquals(List.of("question-1"), llmClient.questions),
                () -> assertEquals(MessageStatus.Done, messageStore.findById("message-1").orElseThrow().getStatus()),
                () -> assertFalse(queueOwnershipService.isOwner("user-1"))
        );
    }

    @Test
    void shouldNotProcessNewMessageWhenAnotherWorkerAcquiresOwnershipAfterRelease() {
        var anotherOwnershipService = new QueueOwnershipService(queueOwnerStore, "owner-2");
        boolean[] messageAdded = {false};
        queueOwnerStore.onRelease = () -> {
            if (!messageAdded[0]) {
                messageAdded[0] = true;
                addQueuedMessage(message("message-1", "user-1", "question-1"));
                assertTrue(anotherOwnershipService.tryAcquire("user-1"));
            }
        };
        assertTrue(queueOwnershipService.tryAcquire("user-1"));

        queueWorker.processQueue("user-1");

        assertAll(
                () -> assertTrue(llmClient.questions.isEmpty()),
                () -> assertEquals(MessageStatus.Queued, messageStore.findById("message-1").orElseThrow().getStatus()),
                () -> assertTrue(anotherOwnershipService.isOwner("user-1")),
                () -> assertFalse(queueOwnershipService.isOwner("user-1"))
        );
    }

    @Test
    void shouldReleaseOwnershipWhenQueueIsEmpty() {
        assertTrue(queueOwnershipService.tryAcquire("user-1"));

        queueWorker.processQueue("user-1");

        assertFalse(queueOwnershipService.isOwner("user-1"));
    }

    @Test
    void shouldNotProcessQueueOwnedByAnotherWorker() {
        var anotherOwnershipService = new QueueOwnershipService(queueOwnerStore, "owner-2");
        assertTrue(anotherOwnershipService.tryAcquire("user-1"));
        addQueuedMessage(message("message-1", "user-1", "question-1"));

        queueWorker.processQueue("user-1");

        assertAll(
                () -> assertTrue(llmClient.questions.isEmpty()),
                () -> assertEquals(MessageStatus.Queued, messageStore.findById("message-1").orElseThrow().getStatus()),
                () -> assertEquals(1, messageQueue.size("user-1")),
                () -> assertTrue(anotherOwnershipService.isOwner("user-1"))
        );
    }

    private void addQueuedMessage(RedisMessage message) {
        messageStore.putInitial(message);
        messageQueue.enqueue(message);
    }

    private RedisMessage message(String messageId, String userId, String text) {
        return new RedisMessage(messageId, userId, text, MessageStatus.Queued, null);
    }

    private static class InMemoryMessageQueue implements MessageQueue {

        private final Map<String, ArrayDeque<String>> queues = new HashMap<>();

        @Override
        public void enqueue(RedisMessage message) {
            queues.computeIfAbsent(message.getUserId(), ignored -> new ArrayDeque<>()).addLast(message.getMessageId());
        }

        @Override
        public List<String> messageIds(String userId) {
            ArrayDeque<String> queue = queues.get(userId);
            return queue == null ? List.of() : List.copyOf(queue);
        }

        @Override
        public boolean isEmpty(String userId) {
            return size(userId) == 0;
        }

        @Override
        public long size(String userId) {
            ArrayDeque<String> queue = queues.get(userId);
            return queue == null ? 0 : queue.size();
        }
    }

    private static class InMemoryMessageStore implements MessageStore {

        private final Map<String, RedisMessage> messages = new HashMap<>();
        private final List<RedisMessage> savedMessages = new ArrayList<>();

        @Override
        public RedisMessage save(RedisMessage message) {
            RedisMessage savedMessage = copy(message);
            messages.put(message.getMessageId(), savedMessage);
            savedMessages.add(savedMessage);
            return message;
        }

        @Override
        public Optional<RedisMessage> findById(String messageId) {
            return Optional.ofNullable(messages.get(messageId));
        }

        private void putInitial(RedisMessage message) {
            messages.put(message.getMessageId(), copy(message));
        }

        private List<MessageStatus> statuses(String messageId) {
            return savedMessages.stream()
                    .filter(message -> messageId.equals(message.getMessageId()))
                    .map(RedisMessage::getStatus)
                    .toList();
        }

        private RedisMessage copy(RedisMessage message) {
            return new RedisMessage(
                    message.getMessageId(),
                    message.getUserId(),
                    message.getText(),
                    message.getStatus(),
                    message.getAnswer()
            );
        }
    }

    private static class InMemoryQueueOwnerStore implements QueueOwnerStore {

        private final Map<String, String> owners = new HashMap<>();
        private Runnable onRelease = () -> {
        };

        @Override
        public boolean setOwnerIfAbsent(String key, String ownerId) {
            if (owners.containsKey(key)) {
                return false;
            }

            owners.put(key, ownerId);
            return true;
        }

        @Override
        public String findOwner(String key) {
            return owners.get(key);
        }

        @Override
        public void releaseIfOwner(String key, String ownerId) {
            if (ownerId.equals(owners.get(key))) {
                owners.remove(key);
                onRelease.run();
            }
        }
    }

    private static class RecordingLlmClient implements LlmClient {

        private final List<String> questions = new ArrayList<>();

        @Override
        public String ask(String userId, String question) {
            questions.add(question);
            return "answer for " + question;
        }
    }
}
