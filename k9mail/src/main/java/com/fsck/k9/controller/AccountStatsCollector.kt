package com.fsck.k9.controller

import android.content.Context
import android.net.Uri
import com.fsck.k9.Account
import com.fsck.k9.AccountStats
import com.fsck.k9.K9
import com.fsck.k9.Preferences
import com.fsck.k9.helper.Utility
import com.fsck.k9.mail.MessagingException
import com.fsck.k9.provider.EmailProvider
import com.fsck.k9.search.LocalSearch
import com.fsck.k9.search.SearchAccount
import com.fsck.k9.search.SqlQueryBuilder
import java.util.ArrayList

interface AccountStatsCollector {
    @Throws(MessagingException::class)
    fun getStats(account: Account): AccountStats?

    fun getSearchAccountStats(searchAccount: SearchAccount): AccountStats
}

internal class DefaultAccountStatsCollector(private val context: Context) : AccountStatsCollector {

    override fun getStats(account: Account): AccountStats? {
        if (!account.isAvailable(context)) {
            return null
        }

        val stats = AccountStats()

        val uri = Uri.withAppendedPath(EmailProvider.CONTENT_URI, "account/" + account.uuid + "/stats")
        val projection = arrayOf(EmailProvider.StatsColumns.UNREAD_COUNT, EmailProvider.StatsColumns.FLAGGED_COUNT)

        // Create LocalSearch instance to exclude special folders (Trash, Drafts, Spam, Outbox, Sent) and
        // limit the search to displayable folders.
        val search = LocalSearch()
        account.excludeSpecialFolders(search)
        account.limitToDisplayableFolders(search)

        // Use the LocalSearch instance to create a WHERE clause to query the content provider
        val query = StringBuilder()
        val queryArgs = ArrayList<String>()
        SqlQueryBuilder.buildWhereClause(account, search.conditions, query, queryArgs)

        val selection = query.toString()
        val selectionArgs = queryArgs.toTypedArray()

        val cursor = context.contentResolver.query(uri, projection, selection, selectionArgs, null)
        try {
            if (cursor != null && cursor.moveToFirst()) {
                stats.unreadMessageCount = cursor.getInt(0)
                stats.flaggedMessageCount = cursor.getInt(1)
            }
        } finally {
            Utility.closeQuietly(cursor)
        }

        if (K9.measureAccounts()) {
            stats.size = account.localStore.size
        }

        return stats
    }

    override fun getSearchAccountStats(searchAccount: SearchAccount): AccountStats {
        val preferences = Preferences.getPreferences(context)
        val search = searchAccount.relatedSearch

        // Collect accounts that belong to the search
        val accountUuids = search.accountUuids
        val accounts: MutableList<Account>
        if (search.searchAllAccounts()) {
            accounts = preferences.accounts
        } else {
            accounts = ArrayList(accountUuids.size)
            var i = 0
            val len = accountUuids.size
            while (i < len) {
                val accountUuid = accountUuids[i]
                accounts[i] = preferences.getAccount(accountUuid)
                i++
            }
        }

        val cr = context.contentResolver

        var unreadMessageCount = 0
        var flaggedMessageCount = 0

        val projection = arrayOf(EmailProvider.StatsColumns.UNREAD_COUNT, EmailProvider.StatsColumns.FLAGGED_COUNT)

        for (account in accounts) {
            val query = StringBuilder()
            val queryArgs = ArrayList<String>()
            val conditions = search.conditions
            SqlQueryBuilder.buildWhereClause(account, conditions, query, queryArgs)

            val selection = query.toString()
            val selectionArgs = queryArgs.toTypedArray()

            val uri = Uri.withAppendedPath(EmailProvider.CONTENT_URI,
                    "account/" + account.uuid + "/stats")

            // Query content provider to get the account stats
            val cursor = cr.query(uri, projection, selection, selectionArgs, null)
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    unreadMessageCount += cursor.getInt(0)
                    flaggedMessageCount += cursor.getInt(1)
                }
            } finally {
                cursor?.close()
            }
        }

        // Create AccountStats instance...
        val stats = AccountStats()
        stats.unreadMessageCount = unreadMessageCount
        stats.flaggedMessageCount = flaggedMessageCount

        return stats
    }
}
