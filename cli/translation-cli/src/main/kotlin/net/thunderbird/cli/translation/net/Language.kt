package net.thunderbird.cli.translation.net

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Language(
    val code: String,
    @SerialName("translated_percent")
    val translatedPercent: Double,
)
