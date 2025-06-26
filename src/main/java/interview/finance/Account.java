package interview.finance;

import java.util.concurrent.locks.ReentrantLock;

public class Account {
    private final String id;
    private long balance;
    private final ReentrantLock lock;

    public Account(String id, long balance) {
        this.id = id;
        this.balance = balance;
        this.lock = new ReentrantLock();
    }

    public String getId() {
        return id;
    }

    public long getBalance() {
        lock.lock();
        try {
            return balance;
        } finally {
            lock.unlock();
        }
    }

    public void deposit(long amount) {
        this.balance += amount;
    }

    public void withdraw(long amount) {
        this.balance -= amount;
    }

    public ReentrantLock getLock() {
        return lock;
    }
}
