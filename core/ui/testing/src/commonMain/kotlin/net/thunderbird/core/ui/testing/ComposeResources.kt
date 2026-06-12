package net.thunderbird.core.ui.testing

import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString as getComposeString

/**
 * Loads a Compose [StringResource] for non-composable tests.
 */
public fun getString(resource: StringResource): String = runBlocking {
    getComposeString(resource)
}
