package interview.messages.queue;

import interview.messages.model.RedisMessage;

import java.util.List;

public interface MessageQueue {

    void enqueue(RedisMessage message);

    List<String> messageIds(String userId);

    boolean isEmpty(String userId);

    long size(String userId);
}
