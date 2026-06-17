package interview.messages.queue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QueueOwnershipServiceTest {

    private InMemoryQueueOwnerStore queueOwnerStore;
    private QueueOwnershipService service;

    @BeforeEach
    void setUp() {
        queueOwnerStore = new InMemoryQueueOwnerStore();
        service = new QueueOwnershipService(queueOwnerStore, "owner-1");
    }

    @Test
    void shouldAcquireQueueWhenThereIsNoOwner() {
        assertTrue(service.tryAcquire("user-1"));
        assertTrue(service.isOwner("user-1"));
    }

    @Test
    void shouldNotAcquireAlreadyOwnedQueue() {
        assertTrue(service.tryAcquire("user-1"));

        assertFalse(service.tryAcquire("user-1"));
        assertTrue(service.isOwner("user-1"));
    }

    @Test
    void shouldNotAcquireQueueOwnedByAnotherOwner() {
        var anotherService = new QueueOwnershipService(queueOwnerStore, "owner-2");

        assertTrue(anotherService.tryAcquire("user-1"));

        assertFalse(service.tryAcquire("user-1"));
        assertFalse(service.isOwner("user-1"));
    }

    @Test
    void shouldReleaseOnlyOwnQueue() {
        var anotherService = new QueueOwnershipService(queueOwnerStore, "owner-2");

        assertTrue(service.tryAcquire("user-1"));

        anotherService.release("user-1");

        assertTrue(service.isOwner("user-1"));
        assertFalse(anotherService.tryAcquire("user-1"));

        service.release("user-1");

        assertTrue(anotherService.tryAcquire("user-1"));
    }

    private static class InMemoryQueueOwnerStore implements QueueOwnerStore {

        private final Map<String, String> owners = new HashMap<>();

        @Override
        public boolean setOwnerIfAbsent(String key, String ownerId) {
            if (owners.containsKey(key)) {
                return false;
            }

            owners.put(key, ownerId);
            return true;
        }

        @Override
        public String findOwner(String key) {
            return owners.get(key);
        }

        @Override
        public void releaseIfOwner(String key, String ownerId) {
            if (ownerId.equals(owners.get(key))) {
                owners.remove(key);
            }
        }
    }
}
