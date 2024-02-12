package net.thunderbird.cli.translation.net

import com.squareup.moshi.Json

data class Language(
    val code: String,
    @Json(name = "translated_percent")
    val translatedPercent: Double,
)
