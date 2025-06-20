package interview.loadbalancer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import static interview.loadbalancer.LoadBalancer.MAX_COUNT;
import static org.junit.jupiter.api.Assertions.*;


class LoadBalancerTest {

    private LoadBalancer underTest;

    @BeforeEach
    void setup() {
        underTest = new LoadBalancer(new ArrayList<>(), new RandomSelectionStrategy());
    }

    @Test
    void shouldRegisterOneAddress() {
        assertTrue(underTest.register("a1"));
        assertEquals(1, underTest.getAllAddresses().size());
    }

    @Test
    void shouldNotRegisterSameAddressTeice() {
        assertTrue(underTest.register("a1"));
        assertFalse(underTest.register("a1"));
        assertEquals(1, underTest.getAllAddresses().size());
    }

    @Test
    void shouldNotRegisterMoreThanMaxAddresses() {
        for (int i = 0; i < MAX_COUNT; i++) {
            assertTrue(underTest.register("a1" + i));
        }
        assertThrows(RuntimeException.class,
                () -> underTest.register("a11"), "Cannot register more address than " + MAX_COUNT);
    }

    @Test
    void shouldNotRegisterInvalidAddress() {
        assertThrows(RuntimeException.class,
                () -> underTest.register(null), "Input address is not valid");
        assertThrows(RuntimeException.class,
                () -> underTest.register(""), "Input address is not valid");
    }

    @Test
    void shouldThrowExceptionForEmptyList() {
        assertThrows(RuntimeException.class,
                () -> underTest.getAddress(), "No addresses registered yet");
    }

    @Test
    void shouldBeRandomEnough() {
        underTest.register("a1");
        underTest.register("a2");
        underTest.register("a3");

        Set<String> seen = new HashSet<>();
        for (int i = 0; i < 1000; i++) {
            seen.add(underTest.getAddress());
        }

        assertEquals(3, seen.size());
    }

}