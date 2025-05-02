package net.thunderbird.feature.widget.message.list.ui

import android.app.PendingIntent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.PendingIntentCompat
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalContext
import androidx.glance.action.clickable
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.layout.wrapContentHeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.fsck.k9.activity.MessageList
import kotlin.random.Random
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.thunderbird.feature.widget.message.list.MessageListItem

@Suppress("LongMethod")
@Composable
internal fun MessageListItemView(item: MessageListItem) {
    val context = LocalContext.current
    Row(
        GlanceModifier.Companion.fillMaxWidth().wrapContentHeight().clickable {
            CoroutineScope(Dispatchers.IO).launch {
                val intent = MessageList.Companion.actionDisplayMessageIntent(context, item.messageReference)
                PendingIntentCompat.getActivity(
                    context,
                    Random.Default.nextInt(),
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT,
                    false,
                )!!
                    .send()
            }
        },
    ) {
        Spacer(GlanceModifier.Companion.width(8.dp).background(Color(item.accountColor)))
        Column(GlanceModifier.Companion.fillMaxWidth().padding(vertical = 4.dp, horizontal = 4.dp)) {
            Row(GlanceModifier.Companion.fillMaxWidth()) {
                Row(GlanceModifier.Companion.defaultWeight(), horizontalAlignment = Alignment.Companion.Start) {
                    Text(
                        item.subject,
                        style = TextStyle(color = GlanceTheme.colors.primary, fontSize = 16.sp),
                        maxLines = 1,
                    )
                }
                Spacer(GlanceModifier.Companion.width(4.dp))
                Row(horizontalAlignment = Alignment.Companion.End) {
                    Box(
                        GlanceModifier.Companion.background(GlanceTheme.colors.primaryContainer).cornerRadius(8.dp)
                            .padding(2.dp),
                    ) {
                        Text(
                            item.threadCount.toString(),
                            style = TextStyle(color = GlanceTheme.colors.primary, fontSize = 13.sp),
                        )
                    }
                    Spacer(GlanceModifier.Companion.width(4.dp))
                    Text(item.displayDate, style = TextStyle(color = GlanceTheme.colors.primary))
                }
            }
            Spacer(GlanceModifier.Companion.height(2.dp))
            Row {
                Text(
                    item.displayName,
                    style = TextStyle(color = GlanceTheme.colors.primary, fontSize = 15.sp),
                    maxLines = 1,
                )
            }
            Spacer(GlanceModifier.Companion.height(2.dp))
            Row {
                Text(
                    item.preview,
                    style = TextStyle(color = GlanceTheme.colors.primary, fontSize = 13.sp),
                    maxLines = 1,
                )
            }
        }
    }
}
