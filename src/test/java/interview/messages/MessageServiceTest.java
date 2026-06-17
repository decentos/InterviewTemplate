package interview.messages;

import interview.llm.LlmClient;
import interview.messages.dto.SubmitMessageRequest;
import interview.messages.model.RedisMessage;
import interview.messages.queue.MessageQueue;
import interview.messages.queue.QueueOwnerStore;
import interview.messages.queue.QueueOwnershipService;
import interview.messages.storage.MessageStore;
import interview.messages.worker.QueueWorker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.task.TaskExecutor;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MessageServiceTest {

    private InMemoryMessageStore messageStore;
    private CapturingMessageQueue messageQueue;
    private CapturingTaskExecutor taskExecutor;
    private RecordingLlmClient llmClient;
    private MessageService messageService;

    @BeforeEach
    void setUp() {
        messageStore = new InMemoryMessageStore();
        messageQueue = new CapturingMessageQueue();
        var queueOwnerStore = new InMemoryQueueOwnerStore();
        var queueOwnershipService = new QueueOwnershipService(queueOwnerStore);
        taskExecutor = new CapturingTaskExecutor();
        llmClient = new RecordingLlmClient();
        var queueWorker = new QueueWorker(messageQueue, messageStore, queueOwnershipService, llmClient);
        messageService = new MessageService(
                messageStore,
                messageQueue,
                queueOwnershipService,
                queueWorker,
                taskExecutor
        );
    }

    @Test
    void shouldSubmitMessageToQueue() {
        var request = new SubmitMessageRequest("user-1", "hello");

        var response = messageService.submit(request);
        var savedMessage = messageStore.findById(response.messageId()).orElseThrow();

        assertAll(
                () -> assertNotNull(response.messageId()),
                () -> assertEquals(MessageStatus.Queued, response.status()),
                () -> assertEquals("user-1", savedMessage.getUserId()),
                () -> assertEquals("hello", savedMessage.getText()),
                () -> assertEquals(MessageStatus.Queued, savedMessage.getStatus()),
                () -> assertNull(savedMessage.getAnswer()),
                () -> assertSame(savedMessage, messageQueue.lastMessage),
                () -> assertEquals(1, taskExecutor.tasks.size()),
                () -> assertTrue(llmClient.questions.isEmpty())
        );
    }

    @Test
    void shouldStartOnlyOneWorkerForSameUser() {
        messageService.submit(new SubmitMessageRequest("user-1", "question-1"));
        messageService.submit(new SubmitMessageRequest("user-1", "question-2"));

        assertAll(
                () -> assertEquals(1, taskExecutor.tasks.size()),
                () -> assertEquals(2, messageQueue.size("user-1"))
        );
    }

    @Test
    void shouldStartSeparateWorkersForDifferentUsers() {
        messageService.submit(new SubmitMessageRequest("user-1", "question-1"));
        messageService.submit(new SubmitMessageRequest("user-2", "question-2"));

        assertAll(
                () -> assertEquals(2, taskExecutor.tasks.size()),
                () -> assertEquals(1, messageQueue.size("user-1")),
                () -> assertEquals(1, messageQueue.size("user-2"))
        );
    }

    @Test
    void shouldReturnMessageById() {
        var message = new RedisMessage("message-1", "user-1", "hello", MessageStatus.Done, "answer");
        messageStore.save(message);

        var response = messageService.findById("message-1").orElseThrow();

        assertAll(
                () -> assertEquals("message-1", response.messageId()),
                () -> assertEquals("user-1", response.userId()),
                () -> assertEquals("hello", response.text()),
                () -> assertEquals(MessageStatus.Done, response.status()),
                () -> assertEquals("answer", response.answer())
        );
    }

    @Test
    void shouldReturnEmptyWhenMessageDoesNotExist() {
        assertTrue(messageService.findById("unknown").isEmpty());
    }

    private static class InMemoryMessageStore implements MessageStore {

        private final Map<String, RedisMessage> messages = new HashMap<>();

        @Override
        public RedisMessage save(RedisMessage message) {
            messages.put(message.getMessageId(), message);
            return message;
        }

        @Override
        public Optional<RedisMessage> findById(String messageId) {
            return Optional.ofNullable(messages.get(messageId));
        }
    }

    private static class CapturingMessageQueue implements MessageQueue {

        private final Map<String, ArrayDeque<String>> queues = new HashMap<>();
        private RedisMessage lastMessage;

        @Override
        public void enqueue(RedisMessage message) {
            lastMessage = message;
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

    private static class InMemoryQueueOwnerStore implements QueueOwnerStore {

        private final Map<String, String> owners = new HashMap<>();

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
            }
        }
    }

    private static class CapturingTaskExecutor implements TaskExecutor {

        private final List<Runnable> tasks = new ArrayList<>();

        @Override
        public void execute(Runnable task) {
            tasks.add(task);
        }
    }

    private static class RecordingLlmClient implements LlmClient {

        private final List<String> questions = new ArrayList<>();

        @Override
        public String ask(String userId, String question) {
            questions.add(question);
            return "answer";
        }
    }
}
