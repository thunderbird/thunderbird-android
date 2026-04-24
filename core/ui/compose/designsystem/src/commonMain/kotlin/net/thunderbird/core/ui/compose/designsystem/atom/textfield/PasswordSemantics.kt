package net.thunderbird.core.ui.compose.designsystem.atom.textfield

import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics

internal fun Modifier.applyPasswordSemantics(): Modifier = compatPasswordSemantics().semantics {
    contentType = ContentType.Password
}

internal expect fun Modifier.compatPasswordSemantics(): Modifier
