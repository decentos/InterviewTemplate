package interview.messages;

import com.fasterxml.jackson.databind.ObjectMapper;
import interview.llm.grpc.AskRequest;
import interview.llm.grpc.AskResponse;
import interview.llm.grpc.LlmServiceGrpc;
import interview.messages.dto.MessageResponse;
import interview.messages.model.RedisMessage;
import interview.messages.queue.MessageQueue;
import interview.messages.queue.QueueOwnerStore;
import interview.messages.storage.MessageStore;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class MessageFlowIntegrationTest {

    private static final String SERVER_NAME = InProcessServerBuilder.generateName();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private FakeLlmService fakeLlmService;

    @Test
    void shouldProcessMessageThroughRestQueueWorkerAndGrpcLlm() throws Exception {
        String submitBody = """
                {
                  "userId": "user-1",
                  "text": "hello"
                }
                """;

        var submitResult = mockMvc.perform(post("/messages")
                        .contentType("application/json")
                        .content(submitBody))
                .andExpect(status().isAccepted())
                .andExpect(header().exists("Location"))
                .andReturn();

        String messageId = objectMapper.readTree(submitResult.getResponse().getContentAsString())
                .get("messageId")
                .asText();

        MessageResponse response = waitUntilDone(messageId);

        assertAll(
                () -> assertEquals("user-1", response.userId()),
                () -> assertEquals("hello", response.text()),
                () -> assertEquals(MessageStatus.Done, response.status()),
                () -> assertEquals("fake grpc answer for hello", response.answer()),
                () -> assertEquals(List.of("user-1:hello"), fakeLlmService.calls)
        );
    }

    private MessageResponse waitUntilDone(String messageId) throws Exception {
        for (int attempt = 0; attempt < 50; attempt++) {
            var result = mockMvc.perform(get("/messages/{messageId}", messageId))
                    .andExpect(status().isOk())
                    .andReturn();

            MessageResponse response = objectMapper.readValue(
                    result.getResponse().getContentAsString(),
                    MessageResponse.class
            );
            if (response.status() == MessageStatus.Done) {
                return response;
            }

            Thread.sleep(50);
        }

        fail("Message was not processed");
        return null;
    }

    @TestConfiguration
    static class TestConfig {

        @Bean(initMethod = "start", destroyMethod = "shutdownNow")
        Server fakeLlmServer(FakeLlmService fakeLlmService) {
            return InProcessServerBuilder.forName(SERVER_NAME)
                    .directExecutor()
                    .addService(fakeLlmService)
                    .build();
        }

        @Bean
        FakeLlmService fakeLlmService() {
            return new FakeLlmService();
        }

        @Bean(destroyMethod = "shutdownNow")
        @Primary
        ManagedChannel fakeLlmChannel(Server fakeLlmServer) {
            return InProcessChannelBuilder.forName(SERVER_NAME)
                    .directExecutor()
                    .build();
        }

        @Bean
        @Primary
        MessageStore inMemoryMessageStore() {
            return new InMemoryMessageStore();
        }

        @Bean
        @Primary
        MessageQueue inMemoryMessageQueue() {
            return new InMemoryMessageQueue();
        }

        @Bean
        @Primary
        QueueOwnerStore inMemoryQueueOwnerStore() {
            return new InMemoryQueueOwnerStore();
        }

        @Bean
        @Primary
        TaskExecutor taskExecutor() {
            return new SimpleAsyncTaskExecutor("message-flow-test-");
        }
    }

    static class FakeLlmService extends LlmServiceGrpc.LlmServiceImplBase {

        private final List<String> calls = new CopyOnWriteArrayList<>();

        @Override
        public void ask(AskRequest request, StreamObserver<AskResponse> responseObserver) {
            calls.add(request.getUserId() + ":" + request.getQuestion());

            responseObserver.onNext(AskResponse.newBuilder()
                    .setAnswer("fake grpc answer for " + request.getQuestion())
                    .build());
            responseObserver.onCompleted();
        }
    }

    private static class InMemoryMessageStore implements MessageStore {

        private final Map<String, RedisMessage> messages = new ConcurrentHashMap<>();

        @Override
        public RedisMessage save(RedisMessage message) {
            messages.put(message.getMessageId(), copy(message));
            return message;
        }

        @Override
        public Optional<RedisMessage> findById(String messageId) {
            return Optional.ofNullable(messages.get(messageId))
                    .map(this::copy);
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

    private static class InMemoryMessageQueue implements MessageQueue {

        private final Map<String, CopyOnWriteArrayList<String>> queues = new ConcurrentHashMap<>();

        @Override
        public void enqueue(RedisMessage message) {
            queues.computeIfAbsent(message.getUserId(), ignored -> new CopyOnWriteArrayList<>())
                    .add(message.getMessageId());
        }

        @Override
        public List<String> messageIds(String userId) {
            return List.copyOf(queues.getOrDefault(userId, new CopyOnWriteArrayList<>()));
        }

        @Override
        public boolean isEmpty(String userId) {
            return size(userId) == 0;
        }

        @Override
        public long size(String userId) {
            return queues.getOrDefault(userId, new CopyOnWriteArrayList<>()).size();
        }
    }

    private static class InMemoryQueueOwnerStore implements QueueOwnerStore {

        private final Map<String, String> owners = new ConcurrentHashMap<>();

        @Override
        public boolean setOwnerIfAbsent(String key, String ownerId) {
            return owners.putIfAbsent(key, ownerId) == null;
        }

        @Override
        public String findOwner(String key) {
            return owners.get(key);
        }

        @Override
        public void releaseIfOwner(String key, String ownerId) {
            owners.remove(key, ownerId);
        }
    }
}
