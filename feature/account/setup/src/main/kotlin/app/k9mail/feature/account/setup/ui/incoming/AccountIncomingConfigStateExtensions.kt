package app.k9mail.feature.account.setup.ui.incoming

internal val AccountIncomingConfigContract.State.isPasswordFieldVisible: Boolean
    get() = authenticationType.isPasswordRequired
