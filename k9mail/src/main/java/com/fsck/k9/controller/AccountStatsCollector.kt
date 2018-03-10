package com.fsck.k9.controller

import android.content.Context
import android.net.Uri
import com.fsck.k9.Account
import com.fsck.k9.AccountStats
import com.fsck.k9.K9
import com.fsck.k9.helper.Utility
import com.fsck.k9.mail.MessagingException
import com.fsck.k9.provider.EmailProvider
import com.fsck.k9.search.LocalSearch
import com.fsck.k9.search.SqlQueryBuilder
import java.util.ArrayList

interface AccountStatsCollector {
    @Throws(MessagingException::class)
    fun getStats(account: Account): AccountStats?
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
}
