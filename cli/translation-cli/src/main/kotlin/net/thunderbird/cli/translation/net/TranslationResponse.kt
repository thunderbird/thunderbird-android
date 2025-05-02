package net.thunderbird.cli.translation.net

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TranslationResponse(
    val next: String?,
    val results: List<Translation>,
)

@Serializable
data class Translation(
    @SerialName("language_code")
    val languageCode: String,
    val language: TranslationLanguage,
)

@Serializable
data class TranslationLanguage(
    val code: String,
)
