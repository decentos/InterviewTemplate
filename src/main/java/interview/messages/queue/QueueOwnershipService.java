package interview.messages.queue;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Objects;
import java.util.UUID;

@Service
public class QueueOwnershipService {

    private static final String OWNER_KEY_PREFIX = "llm:queue-owner:";

    private final QueueOwnerStore queueOwnerStore;
    private final String ownerId;

    @Autowired
    public QueueOwnershipService(QueueOwnerStore queueOwnerStore) {
        this(queueOwnerStore, UUID.randomUUID().toString());
    }

    QueueOwnershipService(QueueOwnerStore queueOwnerStore, String ownerId) {
        this.queueOwnerStore = queueOwnerStore;
        this.ownerId = ownerId;
    }

    public boolean tryAcquire(String userId) {
        Objects.requireNonNull(userId, "userId must not be null");

        return queueOwnerStore.setOwnerIfAbsent(ownerKey(userId), ownerId);
    }

    public boolean isOwner(String userId) {
        Objects.requireNonNull(userId, "userId must not be null");

        return ownerId.equals(queueOwnerStore.findOwner(ownerKey(userId)));
    }

    public void release(String userId) {
        Objects.requireNonNull(userId, "userId must not be null");

        queueOwnerStore.releaseIfOwner(ownerKey(userId), ownerId);
    }

    static String ownerKey(String userId) {
        return OWNER_KEY_PREFIX + userId;
    }
}
