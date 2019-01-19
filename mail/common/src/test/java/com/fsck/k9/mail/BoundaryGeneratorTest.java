package com.fsck.k9.mail;


import java.util.Random;

import org.junit.Test;
import org.mockito.stubbing.OngoingStubbing;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


public class BoundaryGeneratorTest {
    @Test
    public void generateBoundary_allZeros() throws Exception {
        Random random = createRandom(0);
        BoundaryGenerator boundaryGenerator = new BoundaryGenerator(random);

        String result = boundaryGenerator.generateBoundary();

        assertEquals("----000000000000000000000000000000", result);
    }

    @Test
    public void generateBoundary() throws Exception {
        Random random = createRandom(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22,
                23, 24, 25, 26, 27, 28, 35);
        BoundaryGenerator boundaryGenerator = new BoundaryGenerator(random);

        String result = boundaryGenerator.generateBoundary();

        assertEquals("----0123456789ABCDEFGHIJKLMNOPQRSZ", result);
    }

    private Random createRandom(int... values) {
        Random random = mock(Random.class);
        
        OngoingStubbing<Integer> ongoingStubbing = when(random.nextInt(36));
        for (int value : values) {
            ongoingStubbing = ongoingStubbing.thenReturn(value);
        }
        
        return random;
    }
}
