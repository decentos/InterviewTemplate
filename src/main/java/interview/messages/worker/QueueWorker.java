package interview.messages.worker;

import interview.llm.LlmClient;
import interview.messages.MessageStatus;
import interview.messages.model.RedisMessage;
import interview.messages.queue.MessageQueue;
import interview.messages.queue.QueueOwnershipService;
import interview.messages.storage.MessageStore;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.Optional;

@Service
public class QueueWorker {

    private final MessageQueue messageQueue;
    private final MessageStore messageStore;
    private final QueueOwnershipService queueOwnershipService;
    private final LlmClient llmClient;

    public QueueWorker(
            MessageQueue messageQueue,
            MessageStore messageStore,
            QueueOwnershipService queueOwnershipService,
            LlmClient llmClient
    ) {
        this.messageQueue = messageQueue;
        this.messageStore = messageStore;
        this.queueOwnershipService = queueOwnershipService;
        this.llmClient = llmClient;
    }

    public void processQueue(String userId) {
        Objects.requireNonNull(userId, "userId must not be null");

        if (!queueOwnershipService.isOwner(userId)) {
            return;
        }

        processNextMessage(userId);
    }

    private void processNextMessage(String userId) {
        Optional<RedisMessage> message = findNextQueuedMessage(userId);
        if (message.isEmpty()) {
            releaseAndRestartIfNeeded(userId);
            return;
        }

        processMessage(message.get());
        processNextMessage(userId);
    }

    private void releaseAndRestartIfNeeded(String userId) {
        queueOwnershipService.release(userId);

        Optional<RedisMessage> nextMessage = findNextQueuedMessage(userId);
        if (nextMessage.isEmpty() || !queueOwnershipService.tryAcquire(userId)) {
            return;
        }

        processMessage(nextMessage.get());
        processNextMessage(userId);
    }

    private Optional<RedisMessage> findNextQueuedMessage(String userId) {
        return messageQueue.messageIds(userId).stream()
                .map(messageStore::findById)
                .flatMap(Optional::stream)
                .filter(message -> message.getStatus() == MessageStatus.Queued)
                .findFirst();
    }

    private void processMessage(RedisMessage message) {
        message.setStatus(MessageStatus.Processing);
        messageStore.save(message);

        String answer = llmClient.ask(message.getUserId(), message.getText());

        message.setAnswer(answer);
        message.setStatus(MessageStatus.Done);
        messageStore.save(message);
    }
}
