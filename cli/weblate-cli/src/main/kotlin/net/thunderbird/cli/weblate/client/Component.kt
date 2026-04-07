package net.thunderbird.cli.weblate.client

import kotlinx.serialization.Serializable

@Serializable
data class Component(
    val id: Int,
    val name: String,
)
