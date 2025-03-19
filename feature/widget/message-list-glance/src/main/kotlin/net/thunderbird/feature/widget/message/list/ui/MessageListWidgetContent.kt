package net.thunderbird.feature.widget.message.list.ui

import android.app.PendingIntent
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.PendingIntentCompat
import androidx.glance.ColorFilter
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.clickable
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.background
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import app.k9mail.core.ui.legacy.designsystem.atom.icon.Icons
import com.fsck.k9.activity.MessageCompose
import kotlin.random.Random.Default.nextInt
import kotlinx.collections.immutable.ImmutableList
import net.thunderbird.feature.widget.message.list.MessageListItem
import net.thunderbird.feature.widget.message.list.R

@Composable
internal fun MessageListWidgetContent(
    mails: ImmutableList<MessageListItem>,
    onOpenApp: () -> Unit,
) {
    val context = LocalContext.current
    GlanceTheme(GlanceTheme.colors) {
        Column(GlanceModifier.fillMaxSize().background(GlanceTheme.colors.surface)) {
            Row(
                GlanceModifier.padding(horizontal = 8.dp, vertical = 12.dp).fillMaxWidth()
                    .background(GlanceTheme.colors.primaryContainer)
                    .clickable {
                        onOpenApp()
                    },
            ) {
                Text(
                    context.getString(R.string.message_list_glance_widget_inbox_title),
                    style = TextStyle(color = GlanceTheme.colors.primary, fontSize = 20.sp),
                )
                Spacer(GlanceModifier.defaultWeight())
                Image(
                    ImageProvider(Icons.Outlined.Edit),
                    context.getString(R.string.message_list_glance_widget_compose_action),
                    GlanceModifier.padding(2.dp).padding(end = 6.dp).clickable {
                        val intent = Intent(context, MessageCompose::class.java).apply {
                            action = MessageCompose.ACTION_COMPOSE
                        }
                        PendingIntentCompat.getActivity(
                            context,
                            nextInt(),
                            intent,
                            PendingIntent.FLAG_UPDATE_CURRENT,
                            false,
                        )!!.send()
                    },
                    colorFilter = ColorFilter.tint(GlanceTheme.colors.primary),
                )
            }
            LazyColumn(GlanceModifier.fillMaxSize()) {
                items(mails) {
                    Column {
                        MessageListItemView(it)
                        Spacer(
                            GlanceModifier.height(2.dp).fillMaxWidth()
                                .background(GlanceTheme.colors.surfaceVariant),
                        )
                    }
                }
            }
        }
    }
}
