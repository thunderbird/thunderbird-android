package net.thunderbird.cli.weblate.client

import kotlinx.serialization.Serializable

@Serializable
data class ComponentResponse(
    val next: String?,
    val results: List<Component>,
)
