package net.thunderbird.feature.account.settings.impl.domain

internal interface AccountSettingsDomainContract {

    interface ResourceProvider {

        interface GeneralResourceProvider {
            val nameTitle: () -> String
        }
    }
}
