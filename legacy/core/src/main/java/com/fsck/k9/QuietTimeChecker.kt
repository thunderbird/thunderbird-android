package com.fsck.k9

import java.util.Calendar
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

private const val MINUTES_PER_HOUR = 60
class QuietTimeChecker
@OptIn(ExperimentalTime::class)
constructor(
    private val clock: Clock,
    quietTimeStart: String,
    quietTimeEnd: String,
) {
    private val quietTimeStart: Int = parseTime(quietTimeStart)
    private val quietTimeEnd: Int = parseTime(quietTimeEnd)

    val isQuietTime: Boolean
        get() {
            // If start and end times are the same, we're never quiet
            if (quietTimeStart == quietTimeEnd) {
                return false
            }

            val calendar = Calendar.getInstance()
            @OptIn(ExperimentalTime::class)
            calendar.timeInMillis = clock.now().toEpochMilliseconds()

            val minutesSinceMidnight =
                (calendar[Calendar.HOUR_OF_DAY] * MINUTES_PER_HOUR) + calendar[Calendar.MINUTE]

            return if (quietTimeStart > quietTimeEnd) {
                minutesSinceMidnight >= quietTimeStart || minutesSinceMidnight <= quietTimeEnd
            } else {
                minutesSinceMidnight in quietTimeStart..quietTimeEnd
            }
        }

    companion object {
        private fun parseTime(time: String): Int {
            val parts = time.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            val hour = parts[0].toInt()
            val minute = parts[1].toInt()

            return hour * MINUTES_PER_HOUR + minute
        }
    }
}
