package net.thunderbird.core.featureflag.ui

interface FeatureFlagContract {
//    data class State()
    sealed interface Event
    sealed interface Effect
}
