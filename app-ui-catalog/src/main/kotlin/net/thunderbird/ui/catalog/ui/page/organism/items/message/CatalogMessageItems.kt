package net.thunderbird.ui.catalog.ui.page.organism.items.message

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Slider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import app.k9mail.core.ui.compose.designsystem.atom.DividerHorizontal
import app.k9mail.core.ui.compose.designsystem.atom.text.TextLabelSmall
import app.k9mail.core.ui.compose.designsystem.atom.text.TextTitleMedium
import app.k9mail.core.ui.compose.designsystem.atom.text.TextTitleSmall
import app.k9mail.core.ui.compose.designsystem.atom.textfield.TextFieldOutlined
import app.k9mail.core.ui.compose.designsystem.molecule.input.CheckboxInput
import app.k9mail.core.ui.compose.theme2.MainTheme
import kotlin.math.roundToInt
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import net.thunderbird.core.ui.compose.designsystem.organism.message.ActiveMessageItem
import net.thunderbird.core.ui.compose.designsystem.organism.message.JunkMessageItem
import net.thunderbird.core.ui.compose.designsystem.organism.message.NewMessageItem
import net.thunderbird.core.ui.compose.designsystem.organism.message.ReadMessageItem
import net.thunderbird.core.ui.compose.designsystem.organism.message.UnreadMessageItem
import net.thunderbird.ui.catalog.ui.page.common.list.fullSpanItem
import net.thunderbird.ui.catalog.ui.page.common.list.sectionHeaderItem
import net.thunderbird.ui.catalog.ui.page.common.list.sectionSubtitleItem

@OptIn(ExperimentalMaterial3Api::class)
fun LazyGridScope.messageItems() {
    sectionHeaderItem("Message Item")

    sectionSubtitleItem("Configuration")
    fullSpanItem {
        var sender by remember { mutableStateOf("Sender Name") }
        var subject by remember { mutableStateOf("The subject") }
        var preview by remember { mutableStateOf("The message preview") }
        var hideSection by remember { mutableStateOf(false) }
        var hideAvatar by remember { mutableStateOf(false) }
        var swapSenderAndSubject by remember { mutableStateOf(false) }
        var maxPreviewLines by remember { mutableIntStateOf(1) }
        Column {
            MessageItemConfiguration(
                sender = sender,
                subject = subject,
                preview = preview,
                hideSection = hideSection,
                hideAvatar = hideAvatar,
                swapSenderAndSubject = swapSenderAndSubject,
                maxPreviewLines = maxPreviewLines,
                onSender = { sender = it },
                onSubject = { subject = it },
                onPreview = { preview = it },
                onHideSection = { hideSection = it },
                onHideAvatar = { hideAvatar = it },
                onSwapSenderAndSubject = { swapSenderAndSubject = it },
                onMaxPreviewLines = { maxPreviewLines = it },
            )

            DividerHorizontal(modifier = Modifier.padding(MainTheme.spacings.default))

            CatalogMessageItems(
                hideSection = hideSection,
                sender = sender,
                subject = subject,
                preview = preview,
                hideAvatar = hideAvatar,
                swapSenderAndSubject = swapSenderAndSubject,
                maxPreviewLines = maxPreviewLines,
            )
        }
    }
}

@Composable
fun MessageItemConfiguration(
    sender: String,
    subject: String,
    preview: String,
    hideSection: Boolean,
    hideAvatar: Boolean,
    swapSenderAndSubject: Boolean,
    maxPreviewLines: Int,
    modifier: Modifier = Modifier,
    onSender: (String) -> Unit = {},
    onSubject: (String) -> Unit = {},
    onPreview: (String) -> Unit = {},
    onHideSection: (Boolean) -> Unit = {},
    onHideAvatar: (Boolean) -> Unit = {},
    onSwapSenderAndSubject: (Boolean) -> Unit = {},
    onMaxPreviewLines: (Int) -> Unit = {},
) {
    Column(modifier = modifier) {
        CheckboxInput(
            text = "Hide sections",
            onCheckedChange = onHideSection,
            checked = hideSection,
        )
        CheckboxInput(
            text = "Hide avatar",
            onCheckedChange = onHideAvatar,
            checked = hideAvatar,
        )
        CheckboxInput(
            text = "Swap sender and subject",
            onCheckedChange = onSwapSenderAndSubject,
            checked = swapSenderAndSubject,
        )
        TextFieldOutlined(
            value = sender,
            label = "Sender",
            onValueChange = onSender,
            isSingleLine = true,
            modifier = Modifier.fillMaxWidth().padding(horizontal = MainTheme.spacings.double),
        )
        TextFieldOutlined(
            value = subject,
            label = "Subject",
            onValueChange = onSubject,
            isSingleLine = true,
            modifier = Modifier.fillMaxWidth().padding(horizontal = MainTheme.spacings.double),
        )
        TextFieldOutlined(
            value = preview,
            label = "Preview",
            onValueChange = onPreview,
            isSingleLine = false,
            modifier = Modifier.fillMaxWidth().padding(horizontal = MainTheme.spacings.double),
        )
        Column(modifier = Modifier.padding(horizontal = MainTheme.spacings.double)) {
            TextLabelSmall(text = "Preview lines: $maxPreviewLines")
            Slider(
                value = maxPreviewLines.toFloat(),
                onValueChange = { onMaxPreviewLines(it.roundToInt()) },
                valueRange = 1f..6f,
                steps = 6,
            )
        }
    }
}

@Composable
private fun CatalogMessageItems(
    hideSection: Boolean,
    sender: String,
    subject: String,
    preview: String,
    hideAvatar: Boolean,
    swapSenderAndSubject: Boolean,
    maxPreviewLines: Int,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        CatalogNewMessageItem(
            hideSection = hideSection,
            sender = sender,
            subject = subject,
            preview = preview,
            hideAvatar = hideAvatar,
            swapSenderAndSubject = swapSenderAndSubject,
            maxPreviewLines = maxPreviewLines,
        )
        CatalogUnreadMessageItem(
            hideSection = hideSection,
            sender = sender,
            subject = subject,
            preview = preview,
            hideAvatar = hideAvatar,
            swapSenderAndSubject = swapSenderAndSubject,
            maxPreviewLines = maxPreviewLines,
        )
        CatalogReadMessageItem(
            hideSection = hideSection,
            sender = sender,
            subject = subject,
            preview = preview,
            hideAvatar = hideAvatar,
            swapSenderAndSubject = swapSenderAndSubject,
            maxPreviewLines = maxPreviewLines,
        )
        CatalogActiveMessageItem(
            hideSection = hideSection,
            sender = sender,
            subject = subject,
            preview = preview,
            hideAvatar = hideAvatar,
            swapSenderAndSubject = swapSenderAndSubject,
            maxPreviewLines = maxPreviewLines,
        )
        CatalogJunkMessageItem(
            hideSection = hideSection,
            sender = sender,
            subject = subject,
            preview = preview,
            hideAvatar = hideAvatar,
            swapSenderAndSubject = swapSenderAndSubject,
            maxPreviewLines = maxPreviewLines,
        )
    }
}

@Composable
private fun ColumnScope.CatalogNewMessageItem(
    hideSection: Boolean,
    sender: String,
    subject: String,
    preview: String,
    hideAvatar: Boolean,
    swapSenderAndSubject: Boolean,
    maxPreviewLines: Int,
) {
    if (!hideSection) {
        Section(text = "New Message", modifier = Modifier.padding(vertical = MainTheme.spacings.double))
    }
    var selected by remember { mutableStateOf(false) }
    var favourite by remember { mutableStateOf(false) }
    NewMessageItem(
        sender = sender,
        subject = subject,
        preview = preview,
        receivedAt = @OptIn(ExperimentalTime::class) Clock.System.now()
            .toLocalDateTime(TimeZone.currentSystemDefault()),
        favourite = favourite,
        avatar = {
            if (!hideAvatar) {
                Avatar(
                    sender = sender,
                    enabled = !selected,
                    onClick = { selected = true },
                )
            }
        },
        onClick = {
            if (selected) {
                selected = false
            }
        },
        onFavouriteClick = { favourite = !favourite },
        modifier = Modifier.fillMaxWidth(),
        selected = selected,
        swapSenderWithSubject = swapSenderAndSubject,
        maxPreviewLines = maxPreviewLines,
    )
}

@Composable
private fun ColumnScope.CatalogUnreadMessageItem(
    hideSection: Boolean,
    sender: String,
    subject: String,
    preview: String,
    hideAvatar: Boolean,
    swapSenderAndSubject: Boolean,
    maxPreviewLines: Int,
) {
    if (!hideSection) {
        Section(text = "Unread Message", modifier = Modifier.padding(vertical = MainTheme.spacings.double))
    }
    var selected by remember { mutableStateOf(false) }
    var favourite by remember { mutableStateOf(false) }
    UnreadMessageItem(
        sender = sender,
        subject = subject,
        preview = preview,
        receivedAt = @OptIn(ExperimentalTime::class) Clock.System.now()
            .toLocalDateTime(TimeZone.currentSystemDefault()),
        favourite = favourite,
        avatar = {
            if (!hideAvatar) {
                Avatar(
                    sender = sender,
                    enabled = !selected,
                    onClick = { selected = true },
                )
            }
        },
        onClick = {
            if (selected) {
                selected = false
            }
        },
        onFavouriteClick = { favourite = !favourite },
        modifier = Modifier.fillMaxWidth(),
        selected = selected,
        swapSenderWithSubject = swapSenderAndSubject,
        maxPreviewLines = maxPreviewLines,
    )
}

@Composable
private fun ColumnScope.CatalogReadMessageItem(
    hideSection: Boolean,
    sender: String,
    subject: String,
    preview: String,
    hideAvatar: Boolean,
    swapSenderAndSubject: Boolean,
    maxPreviewLines: Int,
) {
    if (!hideSection) {
        Section(text = "Read Message", modifier = Modifier.padding(vertical = MainTheme.spacings.double))
    }
    var selected by remember { mutableStateOf(false) }
    var favourite by remember { mutableStateOf(false) }
    ReadMessageItem(
        sender = sender,
        subject = subject,
        preview = preview,
        receivedAt = @OptIn(ExperimentalTime::class) Clock.System.now()
            .toLocalDateTime(TimeZone.currentSystemDefault()),
        favourite = favourite,
        avatar = {
            if (!hideAvatar) {
                Avatar(
                    sender = sender,
                    enabled = !selected,
                    onClick = { selected = true },
                )
            }
        },
        onClick = {
            if (selected) {
                selected = false
            }
        },
        onFavouriteClick = { favourite = !favourite },
        modifier = Modifier.fillMaxWidth(),
        selected = selected,
        swapSenderWithSubject = swapSenderAndSubject,
        maxPreviewLines = maxPreviewLines,
    )
}

@Composable
private fun ColumnScope.CatalogActiveMessageItem(
    hideSection: Boolean,
    sender: String,
    subject: String,
    preview: String,
    hideAvatar: Boolean,
    swapSenderAndSubject: Boolean,
    maxPreviewLines: Int,
) {
    if (!hideSection) {
        Section(text = "Active Message", modifier = Modifier.padding(vertical = MainTheme.spacings.double))
    }
    var selected by remember { mutableStateOf(false) }
    var favourite by remember { mutableStateOf(false) }
    ActiveMessageItem(
        sender = sender,
        subject = subject,
        preview = preview,
        receivedAt = @OptIn(ExperimentalTime::class) Clock.System.now()
            .toLocalDateTime(TimeZone.currentSystemDefault()),
        favourite = favourite,
        avatar = {
            if (!hideAvatar) {
                Avatar(
                    sender = sender,
                    enabled = !selected,
                    onClick = { selected = true },
                )
            }
        },
        onClick = {
            if (selected) {
                selected = false
            }
        },
        onFavouriteClick = { favourite = !favourite },
        modifier = Modifier.fillMaxWidth(),
        selected = selected,
        swapSenderWithSubject = swapSenderAndSubject,
        maxPreviewLines = maxPreviewLines,
    )
}

@Composable
private fun ColumnScope.CatalogJunkMessageItem(
    hideSection: Boolean,
    sender: String,
    subject: String,
    preview: String,
    hideAvatar: Boolean,
    swapSenderAndSubject: Boolean,
    maxPreviewLines: Int,
) {
    if (!hideSection) {
        Section(text = "Junk Message", modifier = Modifier.padding(vertical = MainTheme.spacings.double))
    }
    var selected by remember { mutableStateOf(false) }
    JunkMessageItem(
        sender = sender,
        subject = subject,
        preview = preview,
        receivedAt = @OptIn(ExperimentalTime::class) Clock.System.now()
            .toLocalDateTime(TimeZone.currentSystemDefault()),
        avatar = {
            if (!hideAvatar) {
                Avatar(
                    sender = sender,
                    enabled = !selected,
                    onClick = { selected = true },
                )
            }
        },
        onClick = {
            if (selected) {
                selected = false
            }
        },
        modifier = Modifier.fillMaxWidth(),
        selected = selected,
        swapSenderWithSubject = swapSenderAndSubject,
        maxPreviewLines = maxPreviewLines,
    )
}

@Composable
private fun Section(text: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = MainTheme.spacings.double,
                top = MainTheme.spacings.default,
                end = MainTheme.spacings.double,
            ),
    ) {
        TextTitleMedium(
            text = text,
        )
        DividerHorizontal()
    }
}

@Composable
private fun Avatar(
    sender: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(MainTheme.sizes.iconAvatar)
            .clip(CircleShape)
            .background(
                color = MainTheme.colors.primaryContainer.copy(alpha = 0.15f),
                shape = CircleShape,
            )
            .border(width = 1.dp, color = MainTheme.colors.primary, shape = CircleShape)
            .clickable(
                enabled = enabled,
                onClick = onClick,
            ),
    ) {
        val monogram = remember(sender) {
            val parts = sender.split(" ")
            buildString {
                append(parts.first().first())
                if (parts.size > 1) {
                    append(parts.last().first())
                }
            }
        }
        TextTitleSmall(text = monogram, modifier = Modifier.align(Alignment.Center))
    }
}
