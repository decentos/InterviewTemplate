package interview.messages.storage;

import interview.messages.model.RedisMessage;
import org.springframework.data.repository.CrudRepository;

public interface MessageRepository extends CrudRepository<RedisMessage, String> {
}
