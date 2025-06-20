package interview.loadbalancer;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class RandomSelectionStrategy implements SelectionStrategy {

    @Override
    public int select(List<String> addresses) {
        return ThreadLocalRandom.current().nextInt(addresses.size());
    }
}
