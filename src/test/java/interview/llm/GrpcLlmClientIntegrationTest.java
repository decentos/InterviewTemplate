package interview.llm;

import interview.llm.grpc.AskRequest;
import interview.llm.grpc.AskResponse;
import interview.llm.grpc.LlmServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

class GrpcLlmClientIntegrationTest {

    private FakeLlmService fakeLlmService;
    private Server server;
    private ManagedChannel channel;
    private GrpcLlmClient client;

    @BeforeEach
    void setUp() throws Exception {
        String serverName = InProcessServerBuilder.generateName();
        fakeLlmService = new FakeLlmService();
        server = InProcessServerBuilder.forName(serverName)
                .directExecutor()
                .addService(fakeLlmService)
                .build()
                .start();
        channel = InProcessChannelBuilder.forName(serverName)
                .directExecutor()
                .build();
        client = new GrpcLlmClient(channel);
    }

    @AfterEach
    void tearDown() {
        channel.shutdownNow();
        server.shutdownNow();
    }

    @Test
    void shouldCallGrpcLlmService() {
        String answer = client.ask("user-1", "hello");

        assertAll(
                () -> assertEquals("grpc answer for hello", answer),
                () -> assertEquals("user-1", fakeLlmService.lastUserId),
                () -> assertEquals("hello", fakeLlmService.lastQuestion)
        );
    }

    private static class FakeLlmService extends LlmServiceGrpc.LlmServiceImplBase {

        private String lastUserId;
        private String lastQuestion;

        @Override
        public void ask(AskRequest request, StreamObserver<AskResponse> responseObserver) {
            lastUserId = request.getUserId();
            lastQuestion = request.getQuestion();

            responseObserver.onNext(AskResponse.newBuilder()
                    .setAnswer("grpc answer for " + request.getQuestion())
                    .build());
            responseObserver.onCompleted();
        }
    }
}
