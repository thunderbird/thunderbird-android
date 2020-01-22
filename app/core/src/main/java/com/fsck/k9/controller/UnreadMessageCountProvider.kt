package com.fsck.k9.controller

import android.content.Context
import com.fsck.k9.Account
import com.fsck.k9.Preferences
import com.fsck.k9.mail.MessagingException
import com.fsck.k9.mailstore.LocalStoreProvider
import com.fsck.k9.search.AccountSearchConditions
import com.fsck.k9.search.LocalSearch
import com.fsck.k9.search.SearchAccount
import com.fsck.k9.search.getAccounts
import timber.log.Timber

interface UnreadMessageCountProvider {
    fun getUnreadMessageCount(account: Account): Int
    fun getUnreadMessageCount(searchAccount: SearchAccount): Int
}

internal class DefaultUnreadMessageCountProvider(
    private val context: Context,
    private val preferences: Preferences,
    private val accountSearchConditions: AccountSearchConditions,
    private val localStoreProvider: LocalStoreProvider
) : UnreadMessageCountProvider {
    override fun getUnreadMessageCount(account: Account): Int {
        if (!account.isAvailable(context)) {
            return 0
        }

        return try {
            val localStore = localStoreProvider.getInstance(account)

            val search = LocalSearch()
            accountSearchConditions.excludeSpecialFolders(account, search)
            accountSearchConditions.limitToDisplayableFolders(account, search)

            localStore.getUnreadMessageCount(search)
        } catch (e: MessagingException) {
            Timber.e(e, "Unable to getUnreadMessageCount for account: %s", account)
            0
        }
    }

    override fun getUnreadMessageCount(searchAccount: SearchAccount): Int {
        val search = searchAccount.relatedSearch
        val accounts = search.getAccounts(preferences)

        var unreadMessageCount = 0
        for (account in accounts) {
            unreadMessageCount += getUnreadMessageCountWithLocalSearch(account, search)
        }

        return unreadMessageCount
    }

    private fun getUnreadMessageCountWithLocalSearch(account: Account, search: LocalSearch): Int {
        return try {
            val localStore = localStoreProvider.getInstance(account)
            localStore.getUnreadMessageCount(search)
        } catch (e: MessagingException) {
            Timber.e(e, "Unable to getUnreadMessageCount for account: %s", account)
            0
        }
    }
}
