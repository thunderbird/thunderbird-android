package com.fsck.k9.mail

import assertk.assertThat
import assertk.assertions.isEqualTo
import java.util.Random
import org.junit.Test
import org.mockito.kotlin.mock

class BoundaryGeneratorTest {
    @Test
    fun `generateBoundary() with all zeros`() {
        val random = createRandom(0)
        val boundaryGenerator = BoundaryGenerator(random)

        val result = boundaryGenerator.generateBoundary()

        assertThat(result).isEqualTo("----000000000000000000000000000000")
    }

    @Test
    fun generateBoundary() {
        val random = createRandom(
            0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 35,
        )
        val boundaryGenerator = BoundaryGenerator(random)

        val result = boundaryGenerator.generateBoundary()

        assertThat(result).isEqualTo("----0123456789ABCDEFGHIJKLMNOPQRSZ")
    }

    private fun createRandom(vararg values: Int): Random {
        return mock {
            var ongoingStubbing = on { nextInt(36) }
            for (value in values) {
                ongoingStubbing = ongoingStubbing.thenReturn(value)
            }
        }
    }
}
