package com.fsck.k9.ui.messageview

import android.content.Context
import android.print.PrintAttributes
import android.print.PrintManager
import com.fsck.k9.mail.Address
import com.fsck.k9.mail.Message
import com.fsck.k9.mailstore.MessageViewInfo
import com.fsck.k9.view.MessageWebView
import java.text.SimpleDateFormat
import java.util.Locale

class MessagePrinter(
    private val context: Context,
    private val appName: String,
    private val noSubjectText: String,
) {
    fun print(messageViewInfo: MessageViewInfo) {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager ?: return

        val subject = messageViewInfo.message.subject ?: noSubjectText
        val headerHtml = buildHeaderHtml(messageViewInfo)
        val cleanBodyHtml = (messageViewInfo.text ?: "")
            .replace(Regex("(?i)<a\\b[^>]*>"), "")
            .replace(Regex("(?i)</a>"), "")
        val fullHtml = buildFullHtml(headerHtml, cleanBodyHtml)

        val printWebView = MessageWebView(context)
        printWebView.displayHtmlContentWithInlineAttachments(
            htmlText = fullHtml,
            attachmentResolver = null,
            onPageFinishedListener = {
                val jobName = "$appName: $subject"
                val printAdapter = printWebView.createPrintDocumentAdapter(jobName)
                printManager.print(jobName, printAdapter, PrintAttributes.Builder().build())
            },
        )
    }

    private fun buildFullHtml(headerHtml: String, bodyHtml: String): String = """
        <html>
        <head>
            <style>
                @page {
                    margin: 24px;
                }
                .page-wrapper {
                    padding: 24px;
                    font-family: Arial, sans-serif;
                    color: #000;
                }
                .page-wrapper > div p {
                    margin: 4px 0;
                }
            </style>
        </head>
        <body>
            <div class="page-wrapper">
                $headerHtml
                $bodyHtml
            </div>
        </body>
        </html>
    """.trimIndent()

    private fun buildHeaderHtml(messageViewInfo: MessageViewInfo): String {
        val message = messageViewInfo.message

        val subject = (message.subject ?: noSubjectText)
            .replace("<", "&lt;")
            .replace(">", "&gt;")

        val from = message.from
            ?.joinToString(", ") { it.toDisplayString() }
            ?: ""

        val to = message.getRecipients(Message.RecipientType.TO)
            ?.joinToString(", ") { it.toDisplayString() }
            ?: ""

        val date = message.sentDate?.let {
            SimpleDateFormat("M/d/yyyy, h:mm a", Locale.getDefault()).format(it)
        } ?: ""

        return """
            <div style="font-size:15px; margin-bottom:16px;">
                <p><b>Subject:</b> $subject</p>
                <p><b>From:</b> $from</p>
                <p><b>Date:</b> $date</p>
                <p><b>To:</b> $to</p>
            </div>
            <hr style="border:none; border-top:1px solid #ccc; margin:12px 0;">
        """.trimIndent()
    }

    private fun Address.toDisplayString(): String {
        val name = personal ?: ""
        return if (name.isNotBlank() && address.isNotBlank()) {
            "$name &lt;$address&gt;"
        } else {
            address ?: ""
        }
    }
}
