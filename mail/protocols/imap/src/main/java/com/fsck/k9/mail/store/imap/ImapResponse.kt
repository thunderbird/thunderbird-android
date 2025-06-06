package com.fsck.k9.mail.store.imap

import java.io.Serializable

/**
 * Represents a single response from the IMAP server.
 *
 * Tagged responses have a non-null tag.
 * Untagged responses have a null tag.
 * Continuation requests are identified with a `+`.
 * The object will contain all of the available tokens at the time the response is received.
 */
internal class ImapResponse private constructor(
    var callback: ImapResponseCallback?,
    val isContinuationRequested: Boolean,
    val tag: String?,
) : ImapList(), Serializable {

    companion object {
        private const val serialVersionUID: Long = 6886458551615975669L

        @JvmStatic
        fun newContinuationRequest(callback: ImapResponseCallback?): ImapResponse {
            return ImapResponse(callback, true, null)
        }

        @JvmStatic
        fun newUntaggedResponse(callback: ImapResponseCallback?): ImapResponse {
            return ImapResponse(callback, false, null)
        }

        @JvmStatic
        fun newTaggedResponse(callback: ImapResponseCallback?, tag: String): ImapResponse {
            return ImapResponse(callback, false, tag)
        }
    }

    val isTagged: Boolean
        get() = tag != null

    override fun toString(): String {
        val displayTag = if (isContinuationRequested) "+" else tag
        return "#$displayTag# ${super.toString()}"
    }
}
