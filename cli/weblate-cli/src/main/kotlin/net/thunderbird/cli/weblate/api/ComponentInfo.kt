package net.thunderbird.cli.weblate.api

import kotlinx.serialization.Serializable

/**
 * Represents the information of a component in Weblate.
 *
 * @property id The unique identifier of the component.
 * @property name The name of the component.
 * @property url The URL of the component in Weblate.
 * @property slug The slug identifier of the component (suitable for matching/include lists)
 * @property category The category of the component
 */
@Serializable
data class ComponentInfo(
    val id: Int,
    val name: String,
    val slug: String,
    val url: String,
    val category: String?,
)
