package interview.loadbalancer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RoundRobinStrategyTest {

    private RoundRobinStrategy underTest;

    @BeforeEach
    void setup() {
        underTest = new RoundRobinStrategy();
    }

    @Test
    void shouldRegisterOneAddress() {
        List<Integer> indexes = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            indexes.add(underTest.select(List.of("a1", "a2", "a3")));
        }
        assertEquals(0, indexes.get(0));
        assertEquals(1, indexes.get(1));
        assertEquals(2, indexes.get(2));
        assertEquals(0, indexes.get(3));
    }

}