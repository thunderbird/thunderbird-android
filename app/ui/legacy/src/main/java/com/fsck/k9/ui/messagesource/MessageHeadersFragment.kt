package com.fsck.k9.ui.messagesource

import android.graphics.Typeface
import android.os.Bundle
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.fsck.k9.controller.MessageReference
import com.fsck.k9.mail.Header
import com.fsck.k9.mail.internet.MimeUtility
import com.fsck.k9.mailstore.MessageRepository
import com.fsck.k9.ui.R
import com.fsck.k9.ui.withArguments
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class MessageHeadersFragment : Fragment() {
    private val messageRepository: MessageRepository by inject()
    private var sourceText: TextView? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(R.layout.message_view_headers, container, false)
        sourceText = root.findViewById(R.id.message_source)
        sourceText!!.setText(R.string.message_progress_text)
        return root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        val arguments = arguments
        val messageReferenceString = arguments!!.getString(ARG_REFERENCE)
        val messageReference = MessageReference.parse(messageReferenceString)

        GlobalScope.launch(Dispatchers.IO) {
            val headers = messageRepository.getHeaders(messageReference!!)
            GlobalScope.launch(Dispatchers.Main) {
                populateHeadersList(headers)
            }
        }
    }

    private fun populateHeadersList(additionalHeaders: List<Header>) {
        val sb = SpannableStringBuilder()
        var first = true
        for ((name, value) in additionalHeaders) {
            if (!first) {
                sb.append("\n")
            } else {
                first = false
            }
            val boldSpan = StyleSpan(Typeface.BOLD)
            val label = SpannableString("$name: ")
            label.setSpan(boldSpan, 0, label.length, 0)
            sb.append(label)
            sb.append(MimeUtility.unfoldAndDecode(value))
        }
        sourceText!!.text = sb
    }

    companion object {
        private const val ARG_REFERENCE = "reference"

        fun newInstance(reference: MessageReference): MessageHeadersFragment {
            return MessageHeadersFragment().withArguments(
                ARG_REFERENCE to reference.toIdentityString()
            )
        }
    }
}
