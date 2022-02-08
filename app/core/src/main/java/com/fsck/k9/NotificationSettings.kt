package com.fsck.k9

/**
 * Describes how a notification should behave.
 */
class NotificationSettings {
    @get:Synchronized
    @set:Synchronized
    var isRingEnabled = false

    @get:Synchronized
    @set:Synchronized
    var ringtone: String? = null

    @get:Synchronized
    @set:Synchronized
    var isLedEnabled = false

    @get:Synchronized
    @set:Synchronized
    var ledColor = 0

    @get:Synchronized
    @set:Synchronized
    var isVibrateEnabled = false

    @get:Synchronized
    @set:Synchronized
    var vibratePattern = 0

    @get:Synchronized
    @set:Synchronized
    var vibrateTimes = 0

    val vibration: LongArray
        get() = getVibration(vibratePattern, vibrateTimes)

    companion object {
        // These are "off, on" patterns, specified in milliseconds
        private val defaultPattern = longArrayOf(300, 200)
        private val pattern1 = longArrayOf(100, 200)
        private val pattern2 = longArrayOf(100, 500)
        private val pattern3 = longArrayOf(200, 200)
        private val pattern4 = longArrayOf(200, 500)
        private val pattern5 = longArrayOf(500, 500)

        fun getVibration(pattern: Int, times: Int): LongArray {
            val selectedPattern = when (pattern) {
                1 -> pattern1
                2 -> pattern2
                3 -> pattern3
                4 -> pattern4
                5 -> pattern5
                else -> defaultPattern
            }

            val repeatedPattern = LongArray(selectedPattern.size * times)
            for (n in 0 until times) {
                System.arraycopy(selectedPattern, 0, repeatedPattern, n * selectedPattern.size, selectedPattern.size)
            }

            // Do not wait before starting the vibration pattern.
            repeatedPattern[0] = 0

            return repeatedPattern
        }
    }
}
