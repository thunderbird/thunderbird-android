package net.thunderbird.feature.notification

@Suppress("MagicNumber")
enum class VibratePattern(
    /**
     * These are "off, on" patterns, specified in milliseconds.
     */
    val vibrationPattern: LongArray,
) {
    Default(vibrationPattern = longArrayOf(300, 200)),
    Pattern1(vibrationPattern = longArrayOf(100, 200)),
    Pattern2(vibrationPattern = longArrayOf(100, 500)),
    Pattern3(vibrationPattern = longArrayOf(200, 200)),
    Pattern4(vibrationPattern = longArrayOf(200, 500)),
    Pattern5(vibrationPattern = longArrayOf(500, 500)),
    ;

    fun serialize(): Int = when (this) {
        Default -> 0
        Pattern1 -> 1
        Pattern2 -> 2
        Pattern3 -> 3
        Pattern4 -> 4
        Pattern5 -> 5
    }

    companion object {
        fun deserialize(value: Int): VibratePattern = when (value) {
            0 -> Default
            1 -> Pattern1
            2 -> Pattern2
            3 -> Pattern3
            4 -> Pattern4
            5 -> Pattern5
            else -> error("Unknown VibratePattern value: $value")
        }
    }
}
