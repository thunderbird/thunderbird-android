package net.thunderbird.cli.weblate.api

import kotlinx.serialization.Serializable

@Serializable
data class ComponentResponse(
    val next: String?,
    val results: List<Component>,
)
