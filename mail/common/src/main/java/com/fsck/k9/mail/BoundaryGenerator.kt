package com.fsck.k9.mail

import java.security.SecureRandom
import org.jetbrains.annotations.VisibleForTesting

class BoundaryGenerator @VisibleForTesting internal constructor(private val random: SecureRandom) {

    fun generateBoundary(): String {
        return buildString(4 + BOUNDARY_CHARACTER_COUNT) {
            append("----")

            repeat(BOUNDARY_CHARACTER_COUNT) {
                append(BASE36_MAP[random.nextInt(36)])
            }
        }
    }

    companion object {
        private const val BOUNDARY_CHARACTER_COUNT = 30

        private val BASE36_MAP = charArrayOf(
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
            'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J',
            'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T',
            'U', 'V', 'W', 'X', 'Y', 'Z',
        )

        private val INSTANCE = BoundaryGenerator(SecureRandom())

        @JvmStatic
        fun getInstance() = INSTANCE
    }
}
