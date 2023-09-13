package app.k9mail.feature.account.server.settings.ui.outgoing

internal val OutgoingServerSettingsContract.State.isUsernameFieldVisible: Boolean
    get() = authenticationType.isUsernameRequired

internal val OutgoingServerSettingsContract.State.isPasswordFieldVisible: Boolean
    get() = authenticationType.isPasswordRequired
