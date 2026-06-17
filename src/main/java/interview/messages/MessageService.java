package interview.messages;

import interview.messages.dto.MessageResponse;
import interview.messages.dto.SubmitMessageRequest;
import interview.messages.dto.SubmitMessageResponse;
import interview.messages.model.RedisMessage;
import interview.messages.queue.MessageQueue;
import interview.messages.queue.QueueOwnershipService;
import interview.messages.storage.MessageStore;
import interview.messages.worker.QueueWorker;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
public class MessageService {

    private final MessageStore messageStore;
    private final MessageQueue messageQueue;
    private final QueueOwnershipService queueOwnershipService;
    private final QueueWorker queueWorker;
    private final TaskExecutor taskExecutor;

    public MessageService(
            MessageStore messageStore,
            MessageQueue messageQueue,
            QueueOwnershipService queueOwnershipService,
            QueueWorker queueWorker,
            TaskExecutor taskExecutor
    ) {
        this.messageStore = messageStore;
        this.messageQueue = messageQueue;
        this.queueOwnershipService = queueOwnershipService;
        this.queueWorker = queueWorker;
        this.taskExecutor = taskExecutor;
    }

    public SubmitMessageResponse submit(SubmitMessageRequest request) {
        RedisMessage message = new RedisMessage(
                UUID.randomUUID().toString(),
                request.userId(),
                request.text(),
                MessageStatus.Queued,
                null
        );

        RedisMessage savedMessage = messageStore.save(message);
        messageQueue.enqueue(savedMessage);
        startWorkerIfNeeded(savedMessage.getUserId());

        return new SubmitMessageResponse(savedMessage.getMessageId(), savedMessage.getStatus());
    }

    public Optional<MessageResponse> findById(String messageId) {
        return messageStore.findById(messageId)
                .map(this::toResponse);
    }

    private MessageResponse toResponse(RedisMessage message) {
        return new MessageResponse(
                message.getMessageId(),
                message.getUserId(),
                message.getText(),
                message.getStatus(),
                message.getAnswer()
        );
    }

    private void startWorkerIfNeeded(String userId) {
        if (!queueOwnershipService.tryAcquire(userId)) {
            return;
        }

        try {
            taskExecutor.execute(() -> queueWorker.processQueue(userId));
        } catch (RuntimeException exception) {
            queueOwnershipService.release(userId);
            throw exception;
        }
    }
}
