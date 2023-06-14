package app.k9mail.feature.account.setup.ui.autoconfig.view

sealed interface AutoDiscoveryStatusHeaderState {
    object NoSettings : AutoDiscoveryStatusHeaderState
    object Trusted : AutoDiscoveryStatusHeaderState
    object Untrusted : AutoDiscoveryStatusHeaderState
}
