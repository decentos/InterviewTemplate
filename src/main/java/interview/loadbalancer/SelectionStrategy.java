package interview.loadbalancer;

import java.util.List;

public interface SelectionStrategy {
    int select(List<String> addresses);
}
