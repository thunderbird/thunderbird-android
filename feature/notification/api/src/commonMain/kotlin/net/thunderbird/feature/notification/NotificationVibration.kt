package net.thunderbird.feature.notification

data class NotificationVibration(
    val isEnabled: Boolean,
    val pattern: VibratePattern,
    val repeatCount: Int,
) {
    val systemPattern: LongArray
        get() = getSystemPattern(pattern, repeatCount)

    companion object {
        val DEFAULT = NotificationVibration(isEnabled = false, pattern = VibratePattern.Default, repeatCount = 5)

        fun getSystemPattern(vibratePattern: VibratePattern, repeatCount: Int): LongArray {
            val selectedPattern = vibratePattern.vibrationPattern
            val repeatedPattern = LongArray(selectedPattern.size * repeatCount)
            for (n in 0 until repeatCount) {
                selectedPattern.copyInto(
                    destination = repeatedPattern,
                    destinationOffset = n * selectedPattern.size,
                    startIndex = 0,
                    endIndex = selectedPattern.size,
                )
            }

            // Do not wait before starting the vibration pattern.
            repeatedPattern[0] = 0

            return repeatedPattern
        }
    }
}
