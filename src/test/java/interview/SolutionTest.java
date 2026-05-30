package interview;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SolutionTest {

    private Solution solution;

    @BeforeEach
    void setUp() {
        solution = new Solution();
    }

    @ParameterizedTest
    @CsvSource({
            "2, 3, 5",
            "0, 0, 0",
            "-2, 5, 3"
    })
    void shouldAddNumbers(int left, int right, int expected) {
        assertEquals(expected, solution.add(left, right));
    }

    @Test
    @DisplayName("documents boundary behavior")
    void shouldFailOnIntegerOverflow() {
        assertAll(
                () -> assertThrows(ArithmeticException.class, () -> solution.add(Integer.MAX_VALUE, 1)),
                () -> assertThrows(ArithmeticException.class, () -> solution.add(Integer.MIN_VALUE, -1))
        );
    }
}
