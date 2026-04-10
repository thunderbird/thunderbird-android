package net.thunderbird.feature.mail.message.list.ui.component.organism

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import androidx.compose.ui.tooling.preview.datasource.LoremIpsum
import app.k9mail.core.ui.compose.designsystem.PreviewWithThemes
import app.k9mail.core.ui.compose.designsystem.atom.text.TextLabelLarge
import app.k9mail.core.ui.compose.designsystem.atom.text.TextTitleSmall
import kotlin.random.Random
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import net.thunderbird.core.ui.compose.designsystem.atom.icon.Icons
import net.thunderbird.core.ui.compose.theme2.MainTheme
import net.thunderbird.feature.mail.message.list.ui.component.config.MessageBadgeStyle
import net.thunderbird.feature.mail.message.list.ui.component.config.MessageItemAccountIndicator
import net.thunderbird.feature.mail.message.list.ui.component.config.MessageItemConfiguration
import net.thunderbird.feature.mail.message.list.ui.component.config.MessageItemLeadingConfiguration
import net.thunderbird.feature.mail.message.list.ui.component.config.MessageItemTrailingConfiguration
import net.thunderbird.feature.mail.message.list.ui.component.config.MessageItemTrailingElement
import net.thunderbird.feature.mail.message.list.ui.component.config.MessageSublineConfiguration
import net.thunderbird.feature.mail.message.list.ui.component.config.MessageSublineLeadingIndicator
import net.thunderbird.feature.mail.message.list.ui.component.molecule.MessageConversationCounterBadgeDefaults
import net.thunderbird.feature.mail.message.list.ui.state.Avatar

private class MessageItemPrevParamCol : CollectionPreviewParameterProvider<MessageItemPrevParams>(
    collection = listOf(
        MessageItemPrevParams(
            previewName = "Monogram encrypted favourite",
            sender = "Sender Name",
            subject = "The subject",
            excerpt = LoremIpsum(words = 3).values.joinToString(),
            hasAttachments = false,
            selected = false,
            receivedAt = "12:34",
            avatar = Avatar.Monogram("AV"),
            trailingElements = persistentListOf(
                MessageItemTrailingElement.EncryptedBadge,
                MessageItemTrailingElement.FavouriteIconButton(favourite = true),
            ),
        ),
        MessageItemPrevParams(
            previewName = "Monogram with attachment",
            sender = "Sender Name",
            subject = "The subject",
            excerpt = LoremIpsum(words = 5).values.joinToString(),
            hasAttachments = true,
            selected = false,
            receivedAt = "12:34",
            avatar = Avatar.Monogram("AV"),
            avatarColor = Color.Magenta,
        ),
        MessageItemPrevParams(
            previewName = "Icon avatar selected",
            sender = "Sender Name",
            subject = "The subject",
            excerpt = LoremIpsum(words = 10).values.joinToString(),
            hasAttachments = false,
            selected = true,
            receivedAt = "12:34",
            avatar = Avatar.Icon(imageVector = Icons.Outlined.Bank),
            avatarColor = Color.DarkGray,
        ),
        MessageItemPrevParams(
            previewName = "Selected threaded with attachment",
            sender = "Sender Name",
            subject = "The subject",
            excerpt = LoremIpsum(words = 20).values.joinToString(),
            hasAttachments = true,
            threadCount = Random.nextInt(2, 100),
            selected = true,
            receivedAt = "12:34",
        ),
        MessageItemPrevParams(
            previewName = "No excerpt threaded",
            sender = "Sender Name",
            subject = "The subject",
            excerpt = "",
            hasAttachments = true,
            threadCount = Random.nextInt(2, 100),
            selected = true,
            receivedAt = "12:34",
            maxExcerptLines = 0,
        ),
        MessageItemPrevParams(
            previewName = "New badge no excerpt",
            sender = "Sender Name",
            subject = "The subject",
            excerpt = "",
            hasAttachments = true,
            threadCount = Random.nextInt(2, 100),
            selected = true,
            receivedAt = "12:34",
            maxExcerptLines = 0,
            badgeStyle = MessageBadgeStyle.New,
        ),
        MessageItemPrevParams(
            previewName = "New badge icon avatar",
            sender = "Sender Name",
            subject = "The subject",
            excerpt = "",
            hasAttachments = true,
            threadCount = Random.nextInt(2, 100),
            selected = false,
            receivedAt = "12:34",
            maxExcerptLines = 0,
            badgeStyle = MessageBadgeStyle.New,
            avatar = Avatar.Icon(imageVector = Icons.Outlined.Bank),
            avatarColor = Color.DarkGray,
        ),
        MessageItemPrevParams(
            previewName = "Unread long excerpt",
            sender = "Sender Name",
            subject = "The subject",
            excerpt = LoremIpsum(words = 100).values.joinToString { it.replace("\n", "") },
            hasAttachments = true,
            threadCount = Random.nextInt(2, 100),
            selected = false,
            receivedAt = "12:34",
            maxExcerptLines = 5,
            avatar = Avatar.Icon(imageVector = Icons.Outlined.Bank),
            badgeStyle = MessageBadgeStyle.Unread,
        ),
        MessageItemPrevParams(
            previewName = "No avatar",
            sender = "Sender Name",
            subject = "The subject",
            excerpt = LoremIpsum(words = 3).values.joinToString(),
            hasAttachments = false,
            selected = false,
            receivedAt = "12:34",
        ),
        MessageItemPrevParams(
            previewName = "No avatar new badge",
            sender = "Sender Name",
            subject = "The subject",
            excerpt = LoremIpsum(words = 3).values.joinToString(),
            hasAttachments = false,
            selected = false,
            receivedAt = "12:34",
            badgeStyle = MessageBadgeStyle.New,
        ),
    ),
) {
    override fun getDisplayName(index: Int): String = values.elementAt(index).previewName
}

@Preview
@Composable
private fun PreviewDefault(
    @PreviewParameter(MessageItemPrevParamCol::class) params: MessageItemPrevParams,
) {
    PreviewWithThemes {
        MessageItem(
            firstLine = { TextTitleSmall(text = params.sender) },
            secondaryLine = { prefix, inlineContent ->
                TextLabelLarge(
                    text = buildAnnotatedString {
                        prefix?.let(::append)
                        append(params.subject)
                    },
                    inlineContent = inlineContent,
                )
            },
            excerpt = params.excerpt,
            receivedAt = params.receivedAt,
            configuration = MessageItemConfiguration(
                maxExcerptLines = params.maxExcerptLines,
                leadingConfiguration = MessageItemLeadingConfiguration(
                    badgeStyle = params.badgeStyle,
                    avatar = params.avatar,
                    avatarColor = params.avatarColor,
                ),
                trailingConfiguration = MessageItemTrailingConfiguration(
                    elements = params.trailingElements,
                ),
                accountIndicator = MessageItemAccountIndicator(params.accountColor),
                secondaryLineConfiguration = MessageSublineConfiguration(
                    leadingItems = buildList {
                        if (params.hasAttachments) {
                            add(MessageSublineLeadingIndicator.AttachmentIcon)
                        }
                        if (params.threadCount > 1) {
                            add(
                                MessageSublineLeadingIndicator.ConversationCounterBadge(
                                    count = params.threadCount,
                                    color = MessageConversationCounterBadgeDefaults.newMessageColor(),
                                ),
                            )
                        }
                    }.toPersistentList(),
                ),
                excerptLineConfiguration = MessageSublineConfiguration(
                    leadingItems = buildList {
                        if (params.hasAttachments) {
                            add(MessageSublineLeadingIndicator.AttachmentIcon)
                        }
                        if (params.threadCount > 1) {
                            add(
                                MessageSublineLeadingIndicator.ConversationCounterBadge(
                                    count = params.threadCount,
                                    color = MessageConversationCounterBadgeDefaults.newMessageColor(),
                                ),
                            )
                        }
                    }.toPersistentList(),
                ),
            ),
            onClick = { },
            onLongClick = { },
            onAvatarClick = { },
            onTrailingClick = { },
            modifier = Modifier.padding(MainTheme.spacings.double),
            selected = params.selected,
            colors = MessageItemDefaults.newMessageItemColors(),
            contentPadding = MessageItemDefaults.defaultContentPadding,
        )
    }
}

@Preview
@Composable
private fun PreviewCompact(
    @PreviewParameter(MessageItemPrevParamCol::class) params: MessageItemPrevParams,
) {
    PreviewWithThemes {
        MessageItem(
            firstLine = { TextTitleSmall(text = params.sender) },
            secondaryLine = { prefix, inlineContent ->
                TextLabelLarge(
                    text = buildAnnotatedString {
                        prefix?.let(::append)
                        append(params.subject)
                    },
                    inlineContent = inlineContent,
                )
            },
            excerpt = params.excerpt,
            receivedAt = params.receivedAt,
            configuration = MessageItemConfiguration(
                maxExcerptLines = params.maxExcerptLines,
                leadingConfiguration = MessageItemLeadingConfiguration(
                    badgeStyle = params.badgeStyle,
                    avatar = params.avatar,
                    avatarColor = params.avatarColor,
                ),
                trailingConfiguration = MessageItemTrailingConfiguration(
                    elements = params.trailingElements,
                ),
                accountIndicator = MessageItemAccountIndicator(params.accountColor),
                secondaryLineConfiguration = MessageSublineConfiguration(
                    leadingItems = buildList {
                        if (params.hasAttachments) {
                            add(MessageSublineLeadingIndicator.AttachmentIcon)
                        }
                        if (params.threadCount > 1) {
                            add(
                                MessageSublineLeadingIndicator.ConversationCounterBadge(
                                    count = params.threadCount,
                                    color = MessageConversationCounterBadgeDefaults.newMessageColor(),
                                ),
                            )
                        }
                    }.toPersistentList(),
                ),
                excerptLineConfiguration = MessageSublineConfiguration(
                    leadingItems = buildList {
                        if (params.hasAttachments) {
                            add(MessageSublineLeadingIndicator.AttachmentIcon)
                        }
                        if (params.threadCount > 1) {
                            add(
                                MessageSublineLeadingIndicator.ConversationCounterBadge(
                                    count = params.threadCount,
                                    color = MessageConversationCounterBadgeDefaults.newMessageColor(),
                                ),
                            )
                        }
                    }.toPersistentList(),
                ),
            ),
            onClick = { },
            onLongClick = { },
            onAvatarClick = { },
            onTrailingClick = { },
            modifier = Modifier.padding(MainTheme.spacings.double),
            selected = params.selected,
            colors = MessageItemDefaults.newMessageItemColors(),
            contentPadding = MessageItemDefaults.compactContentPadding,
        )
    }
}

@Preview
@Composable
private fun PreviewRelaxed(
    @PreviewParameter(MessageItemPrevParamCol::class) params: MessageItemPrevParams,
) {
    PreviewWithThemes {
        MessageItem(
            firstLine = { TextTitleSmall(text = params.sender) },
            secondaryLine = { prefix, inlineContent ->
                TextLabelLarge(
                    text = buildAnnotatedString {
                        prefix?.let(::append)
                        append(params.subject)
                    },
                    inlineContent = inlineContent,
                )
            },
            excerpt = params.excerpt,
            receivedAt = params.receivedAt,
            configuration = MessageItemConfiguration(
                maxExcerptLines = params.maxExcerptLines,
                leadingConfiguration = MessageItemLeadingConfiguration(
                    badgeStyle = params.badgeStyle,
                    avatar = params.avatar,
                    avatarColor = params.avatarColor,
                ),
                trailingConfiguration = MessageItemTrailingConfiguration(
                    elements = params.trailingElements,
                ),
                accountIndicator = MessageItemAccountIndicator(params.accountColor),
                secondaryLineConfiguration = MessageSublineConfiguration(
                    leadingItems = buildList {
                        if (params.hasAttachments) {
                            add(MessageSublineLeadingIndicator.AttachmentIcon)
                        }
                        if (params.threadCount > 1) {
                            add(
                                MessageSublineLeadingIndicator.ConversationCounterBadge(
                                    count = params.threadCount,
                                    color = MessageConversationCounterBadgeDefaults.newMessageColor(),
                                ),
                            )
                        }
                    }.toPersistentList(),
                ),
                excerptLineConfiguration = MessageSublineConfiguration(
                    leadingItems = buildList {
                        if (params.hasAttachments) {
                            add(MessageSublineLeadingIndicator.AttachmentIcon)
                        }
                        if (params.threadCount > 1) {
                            add(
                                MessageSublineLeadingIndicator.ConversationCounterBadge(
                                    count = params.threadCount,
                                    color = MessageConversationCounterBadgeDefaults.newMessageColor(),
                                ),
                            )
                        }
                    }.toPersistentList(),
                ),
            ),
            onClick = { },
            onLongClick = { },
            onAvatarClick = { },
            onTrailingClick = { },
            modifier = Modifier.padding(MainTheme.spacings.double),
            selected = params.selected,
            colors = MessageItemDefaults.newMessageItemColors(),
            contentPadding = MessageItemDefaults.relaxedContentPadding,
        )
    }
}

@Preview
@Composable
private fun PreviewDefaultWithoutAccountIndicator(
    @PreviewParameter(MessageItemPrevParamCol::class) params: MessageItemPrevParams,
) {
    PreviewWithThemes {
        MessageItem(
            firstLine = { TextTitleSmall(text = params.sender) },
            secondaryLine = { prefix, inlineContent ->
                TextLabelLarge(
                    text = buildAnnotatedString {
                        prefix?.let(::append)
                        append(params.subject)
                    },
                    inlineContent = inlineContent,
                )
            },
            excerpt = params.excerpt,
            receivedAt = params.receivedAt,
            configuration = MessageItemConfiguration(
                maxExcerptLines = params.maxExcerptLines,
                leadingConfiguration = MessageItemLeadingConfiguration(
                    badgeStyle = params.badgeStyle,
                    avatar = params.avatar,
                    avatarColor = params.avatarColor,
                ),
                trailingConfiguration = MessageItemTrailingConfiguration(
                    elements = params.trailingElements,
                ),
                accountIndicator = null,
                secondaryLineConfiguration = MessageSublineConfiguration(
                    leadingItems = buildList {
                        if (params.hasAttachments) {
                            add(MessageSublineLeadingIndicator.AttachmentIcon)
                        }
                        if (params.threadCount > 1) {
                            add(
                                MessageSublineLeadingIndicator.ConversationCounterBadge(
                                    count = params.threadCount,
                                    color = MessageConversationCounterBadgeDefaults.newMessageColor(),
                                ),
                            )
                        }
                    }.toPersistentList(),
                ),
                excerptLineConfiguration = MessageSublineConfiguration(
                    leadingItems = buildList {
                        if (params.hasAttachments) {
                            add(MessageSublineLeadingIndicator.AttachmentIcon)
                        }
                        if (params.threadCount > 1) {
                            add(
                                MessageSublineLeadingIndicator.ConversationCounterBadge(
                                    count = params.threadCount,
                                    color = MessageConversationCounterBadgeDefaults.newMessageColor(),
                                ),
                            )
                        }
                    }.toPersistentList(),
                ),
            ),
            onClick = { },
            onLongClick = { },
            onAvatarClick = { },
            onTrailingClick = { },
            modifier = Modifier.padding(MainTheme.spacings.double),
            selected = params.selected,
            colors = MessageItemDefaults.newMessageItemColors(),
        )
    }
}
