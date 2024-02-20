package net.thunderbird.cli.translation.net

import com.squareup.moshi.Json

data class TranslationResponse(
    val next: String?,
    val results: List<Translation>,
)

data class Translation(
    @Json(name = "language_code")
    val languageCode: String,
    val language: TranslationLanguage,
)

data class TranslationLanguage(
    val code: String,
)
