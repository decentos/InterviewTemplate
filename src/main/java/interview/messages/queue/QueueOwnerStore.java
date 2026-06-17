package interview.messages.queue;

public interface QueueOwnerStore {

    boolean setOwnerIfAbsent(String key, String ownerId);

    String findOwner(String key);

    void releaseIfOwner(String key, String ownerId);
}
