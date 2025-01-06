package net.thunderbird.feature.widget.message.list

import android.app.PendingIntent
import android.content.Context
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.app.PendingIntentCompat
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import app.k9mail.legacy.account.Account.SortType
import app.k9mail.legacy.search.SearchAccount.Companion.createUnifiedInboxAccount
import com.fsck.k9.CoreResourceProvider
import com.fsck.k9.K9
import com.fsck.k9.activity.MessageList.Companion.intentDisplaySearch
import kotlin.random.Random.Default.nextInt
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.thunderbird.feature.widget.message.list.ui.MessageListWidgetContent
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

internal class MessageListWidget : GlanceAppWidget(), KoinComponent {

    private val messageListLoader: MessageListLoader by inject()
    private val coreResourceProvider: CoreResourceProvider by inject()

    companion object {
        private var lastMailList = emptyList<MessageListItem>()
        private const val MESSAGE_COUNT = 100
    }

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            var mails by remember { mutableStateOf(lastMailList) }

            LaunchedEffect(Unit) {
                CoroutineScope(Dispatchers.IO).launch {
                    val unifiedInboxSearch = createUnifiedInboxAccount(
                        unifiedInboxTitle = coreResourceProvider.searchUnifiedInboxTitle(),
                        unifiedInboxDetail = coreResourceProvider.searchUnifiedInboxDetail(),
                    ).relatedSearch
                    val messageListConfig = MessageListConfig(
                        search = unifiedInboxSearch,
                        showingThreadedList = K9.isThreadedViewEnabled,
                        sortType = SortType.SORT_DATE,
                        sortAscending = false,
                        sortDateAscending = false,
                    )
                    val list = messageListLoader.getMessageList(messageListConfig)
                    mails = list.subList(0, list.size.coerceAtMost(MESSAGE_COUNT))
                    lastMailList = mails
                }
            }

            MessageListWidgetContent(
                mails = mails.toImmutableList(),
                onOpenApp = { openApp(context) },
            )
        }
    }

    private fun openApp(context: Context) {
        val unifiedInboxAccount = createUnifiedInboxAccount(
            unifiedInboxTitle = coreResourceProvider.searchUnifiedInboxTitle(),
            unifiedInboxDetail = coreResourceProvider.searchUnifiedInboxDetail(),
        )
        val intent = intentDisplaySearch(
            context = context,
            search = unifiedInboxAccount.relatedSearch,
            noThreading = true,
            newTask = true,
            clearTop = true,
        ).apply {
            action = nextInt().toString()
        }
        PendingIntentCompat.getActivity(
            context,
            nextInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT,
            false,
        )!!.send()
    }
}
