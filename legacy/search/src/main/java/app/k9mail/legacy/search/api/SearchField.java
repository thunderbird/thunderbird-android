package app.k9mail.legacy.search.api;


///////////////////////////////////////////////////////////////
// SEARCHFIELD enum
///////////////////////////////////////////////////////////////
/*
 * Using an enum in order to have more robust code. Users ( & coders )
 * are prevented from passing illegal fields. No database overhead
 * when invalid fields passed.
 *
 * By result, only the fields in here are searchable.
 *
 * Fields not in here at this moment ( and by effect not searchable ):
 *      id, html_content, internal_date, message_id,
 *      preview, mime_type
 *
 */
public enum SearchField {
    SUBJECT,
    DATE,
    UID,
    FLAG,
    SENDER,
    TO,
    CC,
    FOLDER,
    BCC,
    REPLY_TO,
    MESSAGE_CONTENTS,
    ATTACHMENT_COUNT,
    DELETED,
    THREAD_ID,
    ID,
    INTEGRATE,
    NEW_MESSAGE,
    READ,
    FLAGGED,
    DISPLAY_CLASS
}
