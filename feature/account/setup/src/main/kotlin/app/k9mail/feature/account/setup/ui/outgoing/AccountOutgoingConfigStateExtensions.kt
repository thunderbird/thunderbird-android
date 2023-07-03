package app.k9mail.feature.account.setup.ui.outgoing

internal val AccountOutgoingConfigContract.State.isUsernameFieldVisible: Boolean
    get() = authenticationType.isUsernameRequired

internal val AccountOutgoingConfigContract.State.isPasswordFieldVisible: Boolean
    get() = authenticationType.isPasswordRequired
