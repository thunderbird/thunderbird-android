package net.thunderbird.feature.mail.message.list.ui.component.molecule

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import app.k9mail.core.ui.compose.designsystem.PreviewWithThemesLightDark
import app.k9mail.core.ui.compose.designsystem.atom.text.TextBodySmall
import kotlinx.collections.immutable.persistentListOf
import net.thunderbird.core.ui.compose.theme2.MainTheme
import net.thunderbird.feature.mail.message.list.ui.component.config.MessageItemConfiguration
import net.thunderbird.feature.mail.message.list.ui.component.config.MessageSublineConfiguration
import net.thunderbird.feature.mail.message.list.ui.component.config.MessageSublineLeadingIndicator

private data class MessageBodyContentPreviewData(
    val previewName: String,
    val subject: String,
    val excerpt: String,
    val configuration: MessageItemConfiguration,
)

private val badgeColor = MessageConversationCounterBadgeColor(
    containerColor = Color(color = 0xFFE8EAF6),
    contentColor = Color(color = 0xFF3F51B5),
    borderColor = Color(color = 0xFFC5CAE9),
)

private class MessageBodyContentPreviewProvider : CollectionPreviewParameterProvider<MessageBodyContentPreviewData>(
    listOf(
        MessageBodyContentPreviewData(
            previewName = "Default",
            subject = "Weekly Team Sync",
            excerpt = "Hi everyone, just a reminder about our weekly sync meeting tomorrow at 10 AM.",
            configuration = MessageItemConfiguration(),
        ),
        MessageBodyContentPreviewData(
            previewName = "With attachment",
            subject = "Re: Project deadline update - we need to discuss the timeline changes",
            excerpt = "I've reviewed the updated timeline and I think we should adjust the milestones accordingly.",
            configuration = MessageItemConfiguration(
                excerptLineConfiguration = MessageSublineConfiguration(
                    leadingItems = persistentListOf(
                        MessageSublineLeadingIndicator.AttachmentIcon,
                    ),
                ),
            ),
        ),
        MessageBodyContentPreviewData(
            previewName = "Counter 1 digit",
            subject = "Lunch plans?",
            excerpt = "Anyone free for lunch today? Thinking about trying that new place on 5th street.",
            configuration = MessageItemConfiguration(
                excerptLineConfiguration = MessageSublineConfiguration(
                    leadingItems = persistentListOf(
                        MessageSublineLeadingIndicator.ConversationCounterBadge(
                            count = 5,
                            color = badgeColor,
                        ),
                    ),
                ),
            ),
        ),
        MessageBodyContentPreviewData(
            previewName = "Counter 2 digits",
            subject = "Q1 Report - Final version attached",
            excerpt = "Please find the final Q1 report attached. Let me know if you have any questions.",
            configuration = MessageItemConfiguration(
                excerptLineConfiguration = MessageSublineConfiguration(
                    leadingItems = persistentListOf(
                        MessageSublineLeadingIndicator.ConversationCounterBadge(
                            count = 42,
                            color = badgeColor,
                        ),
                    ),
                ),
            ),
        ),
        MessageBodyContentPreviewData(
            previewName = "Counter 3 digits",
            subject = "Team discussion thread",
            excerpt = "This thread has grown quite long with many participants contributing.",
            configuration = MessageItemConfiguration(
                excerptLineConfiguration = MessageSublineConfiguration(
                    leadingItems = persistentListOf(
                        MessageSublineLeadingIndicator.ConversationCounterBadge(
                            count = 128,
                            color = badgeColor,
                        ),
                    ),
                ),
            ),
        ),
        MessageBodyContentPreviewData(
            previewName = "Counter and attachment",
            subject = "Design review - assets included",
            excerpt = "Please review the attached mockups before our meeting on Friday.",
            configuration = MessageItemConfiguration(
                excerptLineConfiguration = MessageSublineConfiguration(
                    leadingItems = persistentListOf(
                        MessageSublineLeadingIndicator.ConversationCounterBadge(
                            count = 12,
                            color = badgeColor,
                        ),
                        MessageSublineLeadingIndicator.AttachmentIcon,
                    ),
                ),
            ),
        ),
        MessageBodyContentPreviewData(
            previewName = "Excerpt 1 line",
            subject = "Short preview",
            excerpt = "This excerpt is limited to a single line only.",
            configuration = MessageItemConfiguration(
                maxExcerptLines = 1,
            ),
        ),
        MessageBodyContentPreviewData(
            previewName = "Excerpt 3 lines",
            subject = "Extended preview",
            excerpt = "This is a longer excerpt that can span up to three lines, giving the user more context " +
                "about the message content without needing to open it. It should wrap across multiple lines " +
                "in the preview.",
            configuration = MessageItemConfiguration(
                maxExcerptLines = 3,
            ),
        ),
        MessageBodyContentPreviewData(
            previewName = "No excerpt",
            subject = "Important: Server maintenance scheduled for this weekend",
            excerpt = "",
            configuration = MessageItemConfiguration(
                maxExcerptLines = 0,
            ),
        ),
        MessageBodyContentPreviewData(
            previewName = "No excerpt with attachment",
            subject = "Meeting notes attached",
            excerpt = "",
            configuration = MessageItemConfiguration(
                maxExcerptLines = 0,
                secondaryLineConfiguration = MessageSublineConfiguration(
                    leadingItems = persistentListOf(
                        MessageSublineLeadingIndicator.AttachmentIcon,
                    ),
                ),
            ),
        ),
        MessageBodyContentPreviewData(
            previewName = "No excerpt with counter and attachment",
            subject = "Project updates - see attached files",
            excerpt = "",
            configuration = MessageItemConfiguration(
                maxExcerptLines = 0,
                secondaryLineConfiguration = MessageSublineConfiguration(
                    leadingItems = persistentListOf(
                        MessageSublineLeadingIndicator.ConversationCounterBadge(
                            count = 7,
                            color = badgeColor,
                        ),
                        MessageSublineLeadingIndicator.AttachmentIcon,
                    ),
                ),
            ),
        ),
    ),
) {
    override fun getDisplayName(index: Int): String = values.elementAt(index).previewName
}

@PreviewLightDark
@Composable
private fun MessageBodyContentPreview(
    @PreviewParameter(MessageBodyContentPreviewProvider::class) params: MessageBodyContentPreviewData,
) {
    PreviewWithThemesLightDark {
        MessageBodyContent(
            excerpt = params.excerpt,
            configuration = params.configuration,
            subject = { prefixAnnotatedString, inlineContent ->
                TextBodySmall(
                    text = buildAnnotatedString {
                        prefixAnnotatedString?.let { append(it) }
                        append(params.subject)
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    inlineContent = inlineContent,
                )
            },
            modifier = Modifier.padding(
                horizontal = MainTheme.spacings.triple,
                vertical = MainTheme.spacings.quadruple,
            ),
        )
    }
}
