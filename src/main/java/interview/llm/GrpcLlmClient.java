package interview.llm;

import interview.llm.grpc.AskRequest;
import interview.llm.grpc.LlmServiceGrpc;
import io.grpc.Channel;

public class GrpcLlmClient implements LlmClient {

    private final LlmServiceGrpc.LlmServiceBlockingStub llmService;

    public GrpcLlmClient(Channel channel) {
        this(LlmServiceGrpc.newBlockingStub(channel));
    }

    GrpcLlmClient(LlmServiceGrpc.LlmServiceBlockingStub llmService) {
        this.llmService = llmService;
    }

    @Override
    public String ask(String userId, String question) {
        AskRequest request = AskRequest.newBuilder()
                .setUserId(userId)
                .setQuestion(question)
                .build();

        return llmService.ask(request).getAnswer();
    }
}
