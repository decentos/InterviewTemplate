package interview.messages.storage;

import interview.messages.model.RedisMessage;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class RedisMessageStore implements MessageStore {

    private final MessageRepository messageRepository;

    public RedisMessageStore(MessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    @Override
    public RedisMessage save(RedisMessage message) {
        return messageRepository.save(message);
    }

    @Override
    public Optional<RedisMessage> findById(String messageId) {
        return messageRepository.findById(messageId);
    }
}
