package com.fsck.k9.controller

import com.fsck.k9.Account
import com.fsck.k9.Preferences
import com.fsck.k9.mail.MessagingException
import com.fsck.k9.mailstore.LocalStoreProvider
import com.fsck.k9.search.AccountSearchConditions
import com.fsck.k9.search.LocalSearch
import com.fsck.k9.search.SearchAccount
import com.fsck.k9.search.getAccounts
import timber.log.Timber

interface MessageCountsProvider {
    fun getMessageCounts(account: Account): MessageCounts
    fun getMessageCounts(searchAccount: SearchAccount): MessageCounts
}

data class MessageCounts(val unread: Int, val starred: Int)

internal class DefaultMessageCountsProvider(
    private val preferences: Preferences,
    private val accountSearchConditions: AccountSearchConditions,
    private val localStoreProvider: LocalStoreProvider
) : MessageCountsProvider {
    override fun getMessageCounts(account: Account): MessageCounts {
        return try {
            val localStore = localStoreProvider.getInstance(account)

            val search = LocalSearch()
            accountSearchConditions.excludeSpecialFolders(account, search)
            accountSearchConditions.limitToDisplayableFolders(account, search)

            localStore.getMessageCounts(search)
        } catch (e: MessagingException) {
            Timber.e(e, "Unable to getMessageCounts for account: %s", account)
            MessageCounts(0, 0)
        }
    }

    override fun getMessageCounts(searchAccount: SearchAccount): MessageCounts {
        val search = searchAccount.relatedSearch
        val accounts = search.getAccounts(preferences)

        var unreadCount = 0
        var starredCount = 0
        for (account in accounts) {
            val accountMessageCount = getMessageCountsWithLocalSearch(account, search)
            unreadCount += accountMessageCount.unread
            starredCount += accountMessageCount.starred
        }

        return MessageCounts(unreadCount, starredCount)
    }

    private fun getMessageCountsWithLocalSearch(account: Account, search: LocalSearch): MessageCounts {
        return try {
            val localStore = localStoreProvider.getInstance(account)
            localStore.getMessageCounts(search)
        } catch (e: MessagingException) {
            Timber.e(e, "Unable to getMessageCounts for account: %s", account)
            MessageCounts(0, 0)
        }
    }
}
