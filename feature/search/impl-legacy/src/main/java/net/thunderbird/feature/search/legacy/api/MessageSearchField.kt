package net.thunderbird.feature.search.legacy.api

import kotlinx.parcelize.Parcelize

/**
 * Represents a field that can be searched in messages.
 * Each enum value corresponds to a specific message attribute that can be used in search queries.
 *
 * @property fieldName The name of the database column associated with this search field.
 * @property fieldType The type of the search field, which determines how it can be queried.
 * @property customQueryTemplate An optional custom query template for fields that require special handling.
 */
@Parcelize
enum class MessageSearchField(
    override val fieldName: String,
    override val fieldType: SearchFieldType,
    override val customQueryTemplate: String? = null,
) : SearchField {
    CC("cc_list", SearchFieldType.TEXT),
    DATE("date", SearchFieldType.NUMBER),
    FLAG("flags", SearchFieldType.TEXT),
    ID("id", SearchFieldType.NUMBER),
    SENDER("sender_list", SearchFieldType.TEXT),
    SUBJECT("subject", SearchFieldType.TEXT),
    UID("uid", SearchFieldType.TEXT),
    TO("to_list", SearchFieldType.TEXT),
    FOLDER("folder_id", SearchFieldType.NUMBER),
    BCC("bcc_list", SearchFieldType.TEXT),
    REPLY_TO("reply_to_list", SearchFieldType.TEXT),
    MESSAGE_CONTENTS(
        fieldName = "message_contents",
        fieldType = SearchFieldType.CUSTOM,
        customQueryTemplate = "messages.id IN (SELECT docid FROM messages_fulltext WHERE fulltext MATCH ?)",
    ),
    ATTACHMENT_COUNT("attachment_count", SearchFieldType.NUMBER),
    DELETED("deleted", SearchFieldType.NUMBER),
    THREAD_ID("threads.root", SearchFieldType.NUMBER),
    INTEGRATE("integrate", SearchFieldType.NUMBER),
    NEW_MESSAGE("new_message", SearchFieldType.NUMBER),
    READ("read", SearchFieldType.NUMBER),
    FLAGGED("flagged", SearchFieldType.NUMBER),
    VISIBLE("visible", SearchFieldType.NUMBER),
}
