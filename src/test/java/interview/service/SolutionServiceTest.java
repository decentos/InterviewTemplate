package interview.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class SolutionServiceTest {

    @Mock
    private InnerService innerService;
    private SolutionService underTest;

    @BeforeEach
    public void init() {
        underTest = new SolutionService(innerService);
    }

    @Test
    void test() {
        given(innerService.getInnerValue()).willReturn("mock");

        String test = underTest.getResult();
        assertEquals("mock test", test);
    }
}