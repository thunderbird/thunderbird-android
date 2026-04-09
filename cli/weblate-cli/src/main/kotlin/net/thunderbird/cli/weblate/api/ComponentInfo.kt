package net.thunderbird.cli.weblate.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Represents the information of a component in Weblate.
 *
 * @property id The unique identifier of the component.
 * @property name The name of the component.
 * @property url The URL of the component in Weblate.
 * @property slug The slug identifier of the component.
 * @property category The category url of the component.
 * @property linkedComponent The url of the linked component.
 */
@Serializable
data class ComponentInfo(
    val id: Int,
    val name: String,
    val slug: String,
    val url: String,
    val category: String?,
    @SerialName("linked_component")
    val linkedComponent: String?,
)
