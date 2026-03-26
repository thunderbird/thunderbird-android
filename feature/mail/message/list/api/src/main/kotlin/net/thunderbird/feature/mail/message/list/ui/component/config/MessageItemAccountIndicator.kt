package net.thunderbird.feature.mail.message.list.ui.component.config

import androidx.compose.ui.graphics.Color

/**
 * Represents a visual account indicator displayed in a message item.
 *
 * The indicator helps users quickly identify which account the message
 * is associated with in the unified inbox.
 * ```
 * Message Item structure:
 * ┌───────────┬──────────────────────┬──────────┐
 * │  Leading  │  Primary Line [X]    │ Trailing │
 * │   Area    ├──────────────────────┤   Area   │
 * │           │  Secondary Line      │          │
 * │           │  Excerpt Line        │          │
 * └───────────┴──────────────────────┴──────────┘
 * [X] = Position where this indicator is rendered.
 * ```
 *
 * @property color The color used to render the account indicator,
 *  unique to each account.
 */
data class MessageItemAccountIndicator(val color: Color)
