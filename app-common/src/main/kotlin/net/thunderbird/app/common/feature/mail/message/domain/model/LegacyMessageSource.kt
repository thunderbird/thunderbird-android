package net.thunderbird.app.common.feature.mail.message.domain.model

import net.thunderbird.feature.mail.message.MessageSource
import net.thunderbird.piisafe.annotation.PiiSafe
import com.fsck.k9.mail.Message as LegacyMessage

/**
 * A message source that wraps a legacy message format for backward compatibility.
 *
 * This implementation of [MessageSource] preserves the original legacy message representation
 * to avoid data loss when converting between legacy storage formats and the domain model.
 * The wrapped legacy message contains PII and is excluded from logging.
 *
 * @property message The underlying legacy message representation.
 */
@PiiSafe.HasPii
data class LegacyMessageSource(@get:PiiSafe.Hide val message: LegacyMessage) : MessageSource
