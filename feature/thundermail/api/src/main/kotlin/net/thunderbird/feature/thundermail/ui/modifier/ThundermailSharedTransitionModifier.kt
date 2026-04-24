package net.thunderbird.feature.thundermail.ui.modifier

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun Modifier.brandLogoSharedTransition(
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
) = with(sharedTransitionScope) {
    this@brandLogoSharedTransition then Modifier.sharedBounds(
        sharedContentState = rememberSharedContentState("brand-logo"),
        animatedVisibilityScope = animatedVisibilityScope,
        placeholderSize = SharedTransitionScope.PlaceholderSize.AnimatedSize,
    )
}

@Composable
fun Modifier.brandNameSharedTransition(
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
) = with(sharedTransitionScope) {
    this@brandNameSharedTransition then Modifier.sharedBounds(
        sharedContentState = rememberSharedContentState("brand-name"),
        animatedVisibilityScope = animatedVisibilityScope,
        resizeMode = SharedTransitionScope.ResizeMode.scaleToBounds(),
    )
}

@Composable
fun Modifier.headerSharedTransition(
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
) = with(sharedTransitionScope) {
    this@headerSharedTransition then Modifier
        .sharedElement(
            sharedContentState = rememberSharedContentState("header"),
            animatedVisibilityScope = animatedVisibilityScope,
        )
        .renderInSharedTransitionScopeOverlay()
}
