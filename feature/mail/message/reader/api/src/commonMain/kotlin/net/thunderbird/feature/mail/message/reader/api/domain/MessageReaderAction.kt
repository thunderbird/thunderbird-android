package net.thunderbird.feature.mail.message.reader.api.domain

import androidx.compose.ui.graphics.vector.ImageVector
import net.thunderbird.components.ui.bolt.atom.icon.Icons
import org.jetbrains.compose.resources.StringResource
import tfa.feature.mail.message.reader.api.generated.resources.Res
import tfa.feature.mail.message.reader.api.generated.resources.reader_action_reply
import tfa.feature.mail.message.reader.api.generated.resources.reader_action_reply_all

enum class MessageReaderAction(val label: StringResource, val icon: ImageVector) {
    Reply(label = Res.string.reader_action_reply, Icons.Outlined.Reply),
    ReplyAll(label = Res.string.reader_action_reply_all, Icons.Outlined.ReplyAll),
}
