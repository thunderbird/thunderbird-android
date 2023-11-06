package com.fsck.k9.mail

import assertk.assertThat
import assertk.assertions.isEqualTo
import java.security.SecureRandom
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
        val seed = IntRange(0, 28).toList().toIntArray()

        val random = createRandom(*seed, 35)

        val boundaryGenerator = BoundaryGenerator(random)

        val result = boundaryGenerator.generateBoundary()

        assertThat(result).isEqualTo("----0123456789ABCDEFGHIJKLMNOPQRSZ")
    }

    private fun createRandom(vararg values: Int): SecureRandom {
        return mock {
            var ongoingStubbing = on { nextInt(36) }
            for (value in values) {
                ongoingStubbing = ongoingStubbing.thenReturn(value)
            }
        }
    }
}
