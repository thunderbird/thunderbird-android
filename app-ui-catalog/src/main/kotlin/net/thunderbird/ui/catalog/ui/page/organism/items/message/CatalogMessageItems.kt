package net.thunderbird.ui.catalog.ui.page.organism.items.message

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.k9mail.core.ui.compose.designsystem.atom.DividerHorizontal
import app.k9mail.core.ui.compose.designsystem.atom.text.TextLabelSmall
import app.k9mail.core.ui.compose.designsystem.atom.text.TextTitleMedium
import app.k9mail.core.ui.compose.designsystem.atom.text.TextTitleSmall
import app.k9mail.core.ui.compose.designsystem.atom.textfield.TextFieldOutlined
import app.k9mail.core.ui.compose.designsystem.molecule.input.CheckboxInput
import app.k9mail.core.ui.compose.designsystem.organism.snackbar.SnackbarHost
import app.k9mail.core.ui.compose.designsystem.organism.snackbar.rememberSnackbarHostState
import app.k9mail.core.ui.compose.theme2.MainTheme
import kotlin.math.roundToInt
import kotlin.random.Random
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlinx.coroutines.launch
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
        var config by remember {
            mutableStateOf(
                value = MessageItemConfiguration(
                    sender = "Sender Name",
                    subject = "The subject",
                    preview = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Aliquam eleifend, leo at " +
                        "elementum luctus, felis nisl placerat enim, quis aliquam erat nibh gravida eros. Nunc ac " +
                        "elit mauris. Vivamus tristique, nisi eget rutrum condimentum, dui neque bibendum tortor, " +
                        "id fringilla nisi sem eget velit. In euismod leo luctus, tristique ante et, vulputate " +
                        "metus. Integer volutpat pulvinar dictum. Suspendisse et orci quis diam convallis accumsan " +
                        "in non justo.",
                    hideSection = false,
                    hideAvatar = false,
                    swapSenderAndSubject = false,
                    randomizeAttachment = false,
                    maxPreviewLines = 2,
                    showAccountIndicator = true,
                ),
            )
        }
        Column {
            MessageItemConfiguration(
                config = config,
                onSenderChange = { config = config.copy(sender = it) },
                onSubjectChange = { config = config.copy(subject = it) },
                onPreviewChange = { config = config.copy(preview = it) },
                onHideSectionChange = { config = config.copy(hideSection = it) },
                onHideAvatarChange = { config = config.copy(hideAvatar = it) },
                onSwapSenderAndSubjectChange = { config = config.copy(swapSenderAndSubject = it) },
                onRandomizeAttachmentChange = { config = config.copy(randomizeAttachment = it) },
                onMaxPreviewLines = { config = config.copy(maxPreviewLines = it) },
                onShowAccountIndicator = { config = config.copy(showAccountIndicator = it) },
            )
            DividerHorizontal(modifier = Modifier.padding(MainTheme.spacings.default))
            CatalogMessageItems(config = config)
        }
    }
}

private data class MessageItemConfiguration(
    val sender: String,
    val subject: String,
    val preview: String,
    val hideSection: Boolean,
    val hideAvatar: Boolean,
    val swapSenderAndSubject: Boolean,
    val randomizeAttachment: Boolean,
    val maxPreviewLines: Int,
    val showAccountIndicator: Boolean,
)

@Suppress("LongMethod")
@Composable
private fun MessageItemConfiguration(
    config: MessageItemConfiguration,
    modifier: Modifier = Modifier,
    onSenderChange: (String) -> Unit = {},
    onSubjectChange: (String) -> Unit = {},
    onPreviewChange: (String) -> Unit = {},
    onHideSectionChange: (Boolean) -> Unit = {},
    onHideAvatarChange: (Boolean) -> Unit = {},
    onSwapSenderAndSubjectChange: (Boolean) -> Unit = {},
    onRandomizeAttachmentChange: (Boolean) -> Unit = {},
    onMaxPreviewLines: (Int) -> Unit = {},
    onShowAccountIndicator: (Boolean) -> Unit = {},
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(MainTheme.spacings.half),
    ) {
        CheckboxInput(
            text = "Hide sections",
            onCheckedChange = onHideSectionChange,
            checked = config.hideSection,
        )
        CheckboxInput(
            text = "Hide avatar",
            onCheckedChange = onHideAvatarChange,
            checked = config.hideAvatar,
        )
        CheckboxInput(
            text = "Swap sender and subject",
            onCheckedChange = onSwapSenderAndSubjectChange,
            checked = config.swapSenderAndSubject,
        )
        CheckboxInput(
            text = "Randomize attachment",
            onCheckedChange = onRandomizeAttachmentChange,
            checked = config.randomizeAttachment,
        )
        CheckboxInput(
            text = "Show Account Indicator",
            onCheckedChange = onShowAccountIndicator,
            checked = config.showAccountIndicator,
        )
        TextFieldOutlined(
            value = config.sender,
            label = "Sender",
            onValueChange = onSenderChange,
            isSingleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = MainTheme.spacings.double),
        )
        TextFieldOutlined(
            value = config.subject,
            label = "Subject",
            onValueChange = onSubjectChange,
            isSingleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = MainTheme.spacings.double),
        )
        TextFieldOutlined(
            value = config.preview,
            label = "Preview",
            onValueChange = onPreviewChange,
            isSingleLine = false,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = MainTheme.spacings.double),
        )
        Column(modifier = Modifier.padding(horizontal = MainTheme.spacings.double)) {
            TextLabelSmall(text = "Preview lines: ${config.maxPreviewLines}")
            Slider(
                value = config.maxPreviewLines.toFloat(),
                onValueChange = { onMaxPreviewLines(it.roundToInt()) },
                valueRange = 1f..6f,
                steps = 6,
            )
        }
    }
}

@Composable
private fun CatalogMessageItems(config: MessageItemConfiguration, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        CatalogNewMessageItem(config = config)
        CatalogUnreadMessageItem(config = config)
        CatalogReadMessageItem(config = config)
        CatalogActiveMessageItem(config = config)
        CatalogJunkMessageItem(config = config)
    }
}

@Composable
private fun ColumnScope.CatalogNewMessageItem(
    config: MessageItemConfiguration,
) {
    if (!config.hideSection) {
        Section(text = "New Message", modifier = Modifier.padding(vertical = MainTheme.spacings.double))
    }
    var selected by remember { mutableStateOf(false) }
    var favourite by remember { mutableStateOf(false) }
    val snackbarHostState = rememberSnackbarHostState()
    val coroutineScope = rememberCoroutineScope()

    NewMessageItem(
        sender = config.sender,
        subject = config.subject,
        preview = config.preview,
        receivedAt = @OptIn(ExperimentalTime::class) Clock.System.now()
            .toLocalDateTime(TimeZone.currentSystemDefault()),
        favourite = favourite,
        avatar = {
            if (!config.hideAvatar) {
                Avatar(
                    sender = config.sender,
                    enabled = !selected,
                    onClick = { selected = true },
                )
            }
        },
        onClick = {
            coroutineScope.launch {
                snackbarHostState.showSnackbar("Clicked!")
            }
        },
        onLongClick = {
            coroutineScope.launch {
                snackbarHostState.showSnackbar("Long clicked!")
            }
        },
        onLeadingClick = {
            selected = !selected
        },
        onFavouriteChange = { favourite = it },
        modifier = Modifier.fillMaxWidth(),
        selected = selected,
        swapSenderWithSubject = config.swapSenderAndSubject,
        hasAttachments = remember(config.randomizeAttachment) {
            if (config.randomizeAttachment) Random.nextBoolean() else false
        },
        maxPreviewLines = config.maxPreviewLines,
        showAccountIndicator = config.showAccountIndicator,
        accountIndicatorColor = Color.Magenta,
    )

    SnackbarHost(snackbarHostState)
}

@Composable
private fun ColumnScope.CatalogUnreadMessageItem(
    config: MessageItemConfiguration,
) {
    if (!config.hideSection) {
        Section(text = "Unread Message", modifier = Modifier.padding(vertical = MainTheme.spacings.double))
    }
    var selected by remember { mutableStateOf(false) }
    var favourite by remember { mutableStateOf(false) }

    val snackbarHostState = rememberSnackbarHostState()
    val coroutineScope = rememberCoroutineScope()

    UnreadMessageItem(
        sender = config.sender,
        subject = config.subject,
        preview = config.preview,
        receivedAt = @OptIn(ExperimentalTime::class) Clock.System.now()
            .toLocalDateTime(TimeZone.currentSystemDefault()),
        favourite = favourite,
        avatar = {
            if (!config.hideAvatar) {
                Avatar(
                    sender = config.sender,
                    enabled = !selected,
                    onClick = { selected = true },
                )
            }
        },
        onClick = {
            coroutineScope.launch {
                snackbarHostState.showSnackbar("Clicked!")
            }
        },
        onLongClick = {
            coroutineScope.launch {
                snackbarHostState.showSnackbar("Long clicked!")
            }
        },
        onLeadingClick = {
            selected = !selected
        },
        onFavouriteChange = { favourite = it },
        modifier = Modifier.fillMaxWidth(),
        selected = selected,
        swapSenderWithSubject = config.swapSenderAndSubject,
        hasAttachments = remember(config.randomizeAttachment) {
            if (config.randomizeAttachment) Random.nextBoolean() else false
        },
        maxPreviewLines = config.maxPreviewLines,
        showAccountIndicator = config.showAccountIndicator,
        accountIndicatorColor = Color.Magenta,
    )

    SnackbarHost(snackbarHostState)
}

@Composable
private fun ColumnScope.CatalogReadMessageItem(
    config: MessageItemConfiguration,
) {
    if (!config.hideSection) {
        Section(text = "Read Message", modifier = Modifier.padding(vertical = MainTheme.spacings.double))
    }
    var selected by remember { mutableStateOf(false) }
    var favourite by remember { mutableStateOf(false) }

    val snackbarHostState = rememberSnackbarHostState()
    val coroutineScope = rememberCoroutineScope()

    ReadMessageItem(
        sender = config.sender,
        subject = config.subject,
        preview = config.preview,
        receivedAt = @OptIn(ExperimentalTime::class) Clock.System.now()
            .toLocalDateTime(TimeZone.currentSystemDefault()),
        favourite = favourite,
        avatar = {
            if (!config.hideAvatar) {
                Avatar(
                    sender = config.sender,
                    enabled = !selected,
                    onClick = { selected = true },
                )
            }
        },
        onClick = {
            coroutineScope.launch {
                snackbarHostState.showSnackbar("Clicked!")
            }
        },
        onLongClick = {
            coroutineScope.launch {
                snackbarHostState.showSnackbar("Long clicked!")
            }
        },
        onLeadingClick = {
            selected = !selected
        },
        onFavouriteChange = { favourite = it },
        modifier = Modifier.fillMaxWidth(),
        selected = selected,
        swapSenderWithSubject = config.swapSenderAndSubject,
        hasAttachments = remember(config.randomizeAttachment) {
            if (config.randomizeAttachment) Random.nextBoolean() else false
        },
        maxPreviewLines = config.maxPreviewLines,
        showAccountIndicator = config.showAccountIndicator,
        accountIndicatorColor = Color.Magenta,
    )

    SnackbarHost(snackbarHostState)
}

@Composable
private fun ColumnScope.CatalogActiveMessageItem(
    config: MessageItemConfiguration,
) {
    if (!config.hideSection) {
        Section(text = "Active Message", modifier = Modifier.padding(vertical = MainTheme.spacings.double))
    }
    var selected by remember { mutableStateOf(false) }
    var favourite by remember { mutableStateOf(false) }

    val snackbarHostState = rememberSnackbarHostState()
    val coroutineScope = rememberCoroutineScope()

    ActiveMessageItem(
        sender = config.sender,
        subject = config.subject,
        preview = config.preview,
        receivedAt = @OptIn(ExperimentalTime::class) Clock.System.now()
            .toLocalDateTime(TimeZone.currentSystemDefault()),
        favourite = favourite,
        avatar = {
            if (!config.hideAvatar) {
                Avatar(
                    sender = config.sender,
                    enabled = !selected,
                    onClick = { selected = true },
                )
            }
        },
        onClick = {
            coroutineScope.launch {
                snackbarHostState.showSnackbar("Clicked!")
            }
        },
        onLongClick = {
            coroutineScope.launch {
                snackbarHostState.showSnackbar("Long clicked!")
            }
        },
        onLeadingClick = {
            selected = !selected
        },
        onFavouriteChange = { favourite = it },
        modifier = Modifier.fillMaxWidth(),
        selected = selected,
        swapSenderWithSubject = config.swapSenderAndSubject,
        hasAttachments = remember(config.randomizeAttachment) {
            if (config.randomizeAttachment) Random.nextBoolean() else false
        },
        maxPreviewLines = config.maxPreviewLines,
        showAccountIndicator = config.showAccountIndicator,
        accountIndicatorColor = Color.Magenta,
    )

    SnackbarHost(snackbarHostState)
}

@Composable
private fun ColumnScope.CatalogJunkMessageItem(
    config: MessageItemConfiguration,
) {
    if (!config.hideSection) {
        Section(text = "Junk Message", modifier = Modifier.padding(vertical = MainTheme.spacings.double))
    }
    var selected by remember { mutableStateOf(false) }

    val snackbarHostState = rememberSnackbarHostState()
    val coroutineScope = rememberCoroutineScope()

    JunkMessageItem(
        sender = config.sender,
        subject = config.subject,
        preview = config.preview,
        receivedAt = @OptIn(ExperimentalTime::class) Clock.System.now()
            .toLocalDateTime(TimeZone.currentSystemDefault()),
        avatar = {
            if (!config.hideAvatar) {
                Avatar(
                    sender = config.sender,
                    enabled = !selected,
                    onClick = { selected = true },
                )
            }
        },
        onClick = {
            coroutineScope.launch {
                snackbarHostState.showSnackbar("Clicked!")
            }
        },
        onLongClick = {
            coroutineScope.launch {
                snackbarHostState.showSnackbar("Long clicked!")
            }
        },
        onLeadingClick = {
            selected = !selected
        },
        modifier = Modifier.fillMaxWidth(),
        selected = selected,
        swapSenderWithSubject = config.swapSenderAndSubject,
        hasAttachments = remember(config.randomizeAttachment) {
            if (config.randomizeAttachment) Random.nextBoolean() else false
        },
        maxPreviewLines = config.maxPreviewLines,
        showAccountIndicator = config.showAccountIndicator,
        accountIndicatorColor = Color.Magenta,
    )

    SnackbarHost(snackbarHostState)
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
            .clickable(enabled = enabled, onClick = onClick)
            .padding(MainTheme.spacings.half)
            .background(
                color = MainTheme.colors.primaryContainer.copy(alpha = 0.15f),
                shape = CircleShape,
            )
            .border(width = 1.dp, color = MainTheme.colors.primary, shape = CircleShape),
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
