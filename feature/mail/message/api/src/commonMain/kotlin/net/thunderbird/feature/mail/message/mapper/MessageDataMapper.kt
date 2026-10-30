package net.thunderbird.feature.mail.message.mapper

import net.thunderbird.core.architecture.data.AsyncDataMapper
import net.thunderbird.feature.mail.message.Message

interface MessageDataMapper<TDto> : AsyncDataMapper<Message, TDto>
