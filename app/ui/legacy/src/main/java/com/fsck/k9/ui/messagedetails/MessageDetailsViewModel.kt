package com.fsck.k9.ui.messagedetails

import android.content.res.Resources
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fsck.k9.controller.MessageReference
import com.fsck.k9.helper.ClipboardManager
import com.fsck.k9.helper.Contacts
import com.fsck.k9.mail.Address
import com.fsck.k9.mailstore.MessageDate
import com.fsck.k9.mailstore.MessageRepository
import com.fsck.k9.ui.R
import java.text.DateFormat
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

internal class MessageDetailsViewModel(
    private val resources: Resources,
    private val messageRepository: MessageRepository,
    private val contactSettingsProvider: ContactSettingsProvider,
    private val contacts: Contacts,
    private val clipboardManager: ClipboardManager
) : ViewModel() {
    private val dateFormat = DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.MEDIUM, Locale.getDefault())
    private val uiState = MutableStateFlow<MessageDetailsState>(MessageDetailsState.Loading)

    fun loadData(messageReference: MessageReference): StateFlow<MessageDetailsState> {
        viewModelScope.launch(Dispatchers.IO) {
            uiState.value = try {
                val messageDetails = messageRepository.getMessageDetails(messageReference)

                val senderList = messageDetails.sender?.let { listOf(it) } ?: emptyList()
                val messageDetailsUi = MessageDetailsUi(
                    date = buildDisplayDate(messageDetails.date),
                    from = messageDetails.from.toParticipants(),
                    sender = senderList.toParticipants(),
                    replyTo = messageDetails.replyTo.toParticipants(),
                    to = messageDetails.to.toParticipants(),
                    cc = messageDetails.cc.toParticipants(),
                    bcc = messageDetails.bcc.toParticipants()
                )

                MessageDetailsState.DataLoaded(
                    showContactPicture = contactSettingsProvider.isShowContactPicture,
                    details = messageDetailsUi
                )
            } catch (e: Exception) {
                MessageDetailsState.Error
            }
        }

        return uiState
    }

    private fun buildDisplayDate(messageDate: MessageDate): String? {
        return when (messageDate) {
            is MessageDate.InvalidDate -> messageDate.dateHeader
            MessageDate.MissingDate -> null
            is MessageDate.ValidDate -> dateFormat.format(messageDate.date)
        }
    }

    private fun List<Address>.toParticipants(): List<Participant> {
        return this.map { address ->
            Participant(
                address = address,
                contactLookupUri = contacts.getContactUri(address.address)
            )
        }
    }

    fun onCopyEmailAddressToClipboard(participant: Participant) {
        val label = resources.getString(R.string.clipboard_label_email_address)
        val emailAddress = participant.address.address
        clipboardManager.setText(label, emailAddress)
    }

    fun onCopyNameAndEmailAddressToClipboard(participant: Participant) {
        val label = resources.getString(R.string.clipboard_label_name_and_email_address)
        val nameAndEmailAddress = participant.address.toString()
        clipboardManager.setText(label, nameAndEmailAddress)
    }
}

sealed interface MessageDetailsState {
    object Loading : MessageDetailsState
    object Error : MessageDetailsState
    data class DataLoaded(
        val showContactPicture: Boolean,
        val details: MessageDetailsUi
    ) : MessageDetailsState
}
