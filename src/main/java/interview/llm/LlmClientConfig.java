package interview.llm;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LlmClientConfig {

    @Bean(destroyMethod = "shutdown")
    @ConditionalOnMissingBean(ManagedChannel.class)
    public ManagedChannel llmManagedChannel(
            @Value("${llm.grpc.host:localhost}") String host,
            @Value("${llm.grpc.port:50051}") int port
    ) {
        return ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext()
                .build();
    }

    @Bean
    @ConditionalOnMissingBean(LlmClient.class)
    public LlmClient grpcLlmClient(ManagedChannel llmManagedChannel) {
        return new GrpcLlmClient(llmManagedChannel);
    }
}
