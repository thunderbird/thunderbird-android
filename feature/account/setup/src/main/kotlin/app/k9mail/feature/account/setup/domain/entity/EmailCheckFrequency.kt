package app.k9mail.feature.account.setup.domain.entity

import kotlinx.collections.immutable.toImmutableList

@Suppress("MagicNumber")
enum class EmailCheckFrequency(
    val minutes: Int,
) {
    MANUAL(-1),
    EVERY_15_MINUTES(15),
    EVERY_30_MINUTES(30),
    EVERY_HOUR(1.fromHour()),
    EVERY_2_HOURS(2.fromHour()),
    EVERY_3_HOURS(3.fromHour()),
    EVERY_6_HOURS(6.fromHour()),
    EVERY_12_HOURS(12.fromHour()),
    EVERY_24_HOURS(24.fromHour()),
    ;

    companion object {
        val DEFAULT = EVERY_HOUR
        fun all() = entries.toImmutableList()

        fun fromMinutes(minutes: Int): EmailCheckFrequency {
            return all().find { it.minutes == minutes } ?: DEFAULT
        }
    }
}

@Suppress("MagicNumber")
private fun Int.fromHour(): Int {
    return 60 * this
}
