package interview.loadbalancer;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class RoundRobinStrategy implements SelectionStrategy {

    private final AtomicInteger counter;

    public RoundRobinStrategy() {
        this.counter = new AtomicInteger(0);
    }

    @Override
    public int select(List<String> addresses) {
        return Math.abs(counter.getAndIncrement() % addresses.size());
    }
}
