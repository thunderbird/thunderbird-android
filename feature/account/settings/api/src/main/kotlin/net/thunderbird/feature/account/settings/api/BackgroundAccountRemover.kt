package net.thunderbird.feature.account.settings.api

interface BackgroundAccountRemover {
    fun removeAccountAsync(accountUuid: String)
}
