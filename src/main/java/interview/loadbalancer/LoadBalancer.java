package interview.loadbalancer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class LoadBalancer {

    protected static final int MAX_COUNT = 10;
    private final List<String> addresses;
    private final SelectionStrategy selectionStrategy;

    public LoadBalancer(List<String> addresses, SelectionStrategy selectionStrategy) {
        this.addresses = addresses;
        this.selectionStrategy = selectionStrategy;
    }

    public synchronized boolean register(String address) {
        if (!isValidAddress(address)) {
            throw new RuntimeException("Input address is not valid");
        } else if (addresses.size() >= MAX_COUNT) {
            throw new RuntimeException("Cannot register more address than " + MAX_COUNT);
        } else if (addresses.contains(address)) {
            return false;
        }
        return addresses.add(address);
    }

    public String getAddress() {
        if (addresses.isEmpty()) {
            throw new RuntimeException("No addresses registered yet");
        }
        int index = selectionStrategy.select(addresses);
        return addresses.get(index);
    }

    protected List<String> getAllAddresses() {
        return new ArrayList<>(addresses);
    }

    private boolean isValidAddress(String address) {
        return address != null && !address.isEmpty();
    }

}
