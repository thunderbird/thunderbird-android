package com.fsck.k9.ui.compose

import com.fsck.k9.activity.MessageCompose.Action
import com.fsck.k9.message.SimpleMessageFormat
import net.thunderbird.core.android.account.Identity
import net.thunderbird.core.android.account.MessageFormat

class ComposeMessageFormatDecider(
    private val quotedTextState: QuotedTextState,
) {
    fun decide(
        accountMessageFormat: MessageFormat,
        identity: Identity,
        action: Action,
        isForceTextMessageFormat: Boolean,
    ) = when {
        accountMessageFormat == MessageFormat.TEXT -> SimpleMessageFormat.TEXT
        // An edited quote is converted to plain text, so we can't use HTML
        quotedTextState.isForcePlainText() && quotedTextState.includeQuotedText() -> SimpleMessageFormat.TEXT
        // PGP inline doesn't play well with HTML
        isForceTextMessageFormat -> SimpleMessageFormat.TEXT
        accountMessageFormat == MessageFormat.AUTO -> decideAuto(identity, action)
        // accountMessageFormat == MessageFormat.TEXT
        else -> SimpleMessageFormat.HTML
    }

    /**
     * MessageFormat.AUTO picks TEXT when possible, unless the signature or included quote require HTML.
     */
    private fun decideAuto(identity: Identity, action: Action) = when {
        quotedTextState.includeQuotedText() && quotedTextState.isQuotedTextText() -> SimpleMessageFormat.TEXT
        identity.isHtmlSignaturePresent -> SimpleMessageFormat.HTML
        action == Action.COMPOSE -> SimpleMessageFormat.TEXT
        !quotedTextState.includeQuotedText() -> SimpleMessageFormat.TEXT
        else -> SimpleMessageFormat.HTML
    }
}
