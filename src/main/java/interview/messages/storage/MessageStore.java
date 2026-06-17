package interview.messages.storage;

import interview.messages.model.RedisMessage;

import java.util.Optional;

public interface MessageStore {

    RedisMessage save(RedisMessage message);

    Optional<RedisMessage> findById(String messageId);
}
