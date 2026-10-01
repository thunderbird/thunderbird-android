package net.thunderbird.core.featureflag.model

import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

enum class RemoteCatalogFetchFrequency(val frequency: Duration) {
    Short(frequency = 15.minutes),
    Default(frequency = 30.minutes),
    Longer(frequency = 1.hours),
}
