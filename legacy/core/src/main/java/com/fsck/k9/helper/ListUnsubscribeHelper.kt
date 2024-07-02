package com.fsck.k9.helper

import android.net.Uri
import com.fsck.k9.mail.Message
import java.util.regex.Pattern

object ListUnsubscribeHelper {
    private const val LIST_UNSUBSCRIBE_HEADER = "List-Unsubscribe"
    private val MAILTO_CONTAINER_PATTERN = Pattern.compile("<(mailto:.+?)>")
    private val HTTPS_CONTAINER_PATTERN = Pattern.compile("<(https:.+?)>")

    // As K-9 Mail is an email client, we prefer a mailto: unsubscribe method
    // but if none is found, a https URL is acceptable too
    fun getPreferredListUnsubscribeUri(message: Message): UnsubscribeUri? {
        val headerValues = message.getHeader(LIST_UNSUBSCRIBE_HEADER)
        if (headerValues.isEmpty()) {
            return null
        }
        val listUnsubscribeUris = mutableListOf<Uri>()
        for (headerValue in headerValues) {
            val uri = extractUri(headerValue) ?: continue

            if (uri.scheme == "mailto") {
                return MailtoUnsubscribeUri(uri)
            }

            // If we got here it must be HTTPS
            listUnsubscribeUris.add(uri)
        }

        if (listUnsubscribeUris.isNotEmpty()) {
            return HttpsUnsubscribeUri(listUnsubscribeUris[0])
        }

        return null
    }

    private fun extractUri(headerValue: String?): Uri? {
        if (headerValue == null || headerValue.isEmpty()) {
            return null
        }

        var matcher = MAILTO_CONTAINER_PATTERN.matcher(headerValue)
        if (matcher.find()) {
            return Uri.parse(matcher.group(1))
        }

        matcher = HTTPS_CONTAINER_PATTERN.matcher(headerValue)
        if (matcher.find()) {
            return Uri.parse(matcher.group(1))
        }

        return null
    }
}
