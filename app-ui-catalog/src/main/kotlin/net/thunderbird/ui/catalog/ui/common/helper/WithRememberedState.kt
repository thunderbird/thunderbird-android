package net.thunderbird.ui.catalog.ui.common.helper

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember

@Composable
internal fun <T> WithRememberedState(
    input: T,
    content: @Composable (state: MutableState<T>) -> Unit,
) {
    val state = remember { mutableStateOf(input) }
    content(state)
}
