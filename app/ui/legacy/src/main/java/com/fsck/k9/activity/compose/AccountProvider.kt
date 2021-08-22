package com.fsck.k9.activity.compose

import com.fsck.k9.Account
import com.fsck.k9.Identity
import com.fsck.k9.K9
import com.fsck.k9.MissingAccoutException
import com.fsck.k9.Preferences
import com.fsck.k9.controller.MessageReference
import com.fsck.k9.mail.Message
import com.fsck.k9.mailstore.LocalMessage
import com.fsck.k9.mailstore.MessageViewInfo
import com.fsck.k9.message.IdentityField
import com.fsck.k9.message.IdentityHeaderParser
import java.util.HashMap
import java.util.Objects

class AccountProvider {
    var account: Account
    var identity: Identity
        set(identity) {
            identityChanged = field != null && !Objects.equals(field, identity)
            field = identity
        }

    private val preferences: Preferences
    private var identityChanged = false
    var signatureChanged = false

    constructor(preferences: Preferences, accountUuid: String?) {
        var acc: Account? = null
        if (accountUuid != null) {
            acc = preferences.getAccount(accountUuid)
        }

        if (acc == null) {
            acc = preferences.defaultAccount
        }

        if (acc == null) {
            throw MissingAccoutException()
        }
        this.account = acc!!
        this.identity = account.getIdentity(0)
        this.preferences = preferences
    }

    fun onAccountChosen(account: Account, callback: OnAccountSelectedCallback) {
        if (!this.account.equals(account)) {
            val previousAccount = this.account
            this.account = account
            callback.accountChosen(previousAccount, account)
        }
    }

    fun updateAccountAndIdentity(
        messageViewInfo: MessageViewInfo,
        message: Message,
        callback: OnIdentityUpdateFinishedCallback
    ) {
        // Decode the identity header when loading a draft.
        // See buildIdentityHeader(TextBody) for a detailed description of the composition of this blob.

        // Decode the identity header when loading a draft.
        // See buildIdentityHeader(TextBody) for a detailed description of the composition of this blob.
        var draftProperties: Map<IdentityField?, String?> = HashMap()
        var identityHeaders = message.getHeader(K9.IDENTITY_HEADER)
        if (identityHeaders.size == 0) {
            identityHeaders = messageViewInfo.rootPart.getHeader(K9.IDENTITY_HEADER)
        }

        if (identityHeaders.size > 0 && identityHeaders[0] != null) {
            draftProperties = IdentityHeaderParser.parse(identityHeaders[0])
        }

        var newIdentity = Identity()
        if (draftProperties.containsKey(IdentityField.SIGNATURE)) {
            newIdentity = newIdentity
                .withSignatureUse(true)
                .withSignature(draftProperties[IdentityField.SIGNATURE])
            signatureChanged = true
        } else {
            if (message is LocalMessage) {
                newIdentity = newIdentity.withSignatureUse(message.folder.signatureUse)
            }
            newIdentity = newIdentity.withSignature(identity.signature)
        }

        if (draftProperties.containsKey(IdentityField.NAME)) {
            newIdentity = newIdentity.withName(draftProperties[IdentityField.NAME])
            identityChanged = true
        } else {
            newIdentity = newIdentity.withName(identity.name)
        }

        if (draftProperties.containsKey(IdentityField.EMAIL)) {
            newIdentity = newIdentity.withEmail(draftProperties[IdentityField.EMAIL])
            identityChanged = true
        } else {
            newIdentity = newIdentity.withEmail(identity.email)
        }

        var relatedMessageReference: MessageReference? = null
        if (draftProperties.containsKey(IdentityField.ORIGINAL_MESSAGE)) {
            val originalMessage = draftProperties[IdentityField.ORIGINAL_MESSAGE]
            val messageReference = MessageReference.parse(originalMessage)
            if (messageReference != null) {
                // Check if this is a valid account in our database
                val account = preferences.getAccount(messageReference.accountUuid)
                if (account != null) {
                    relatedMessageReference = messageReference
                }
            }
        }

        identity = newIdentity
        callback.onFinished(relatedMessageReference, draftProperties)
    }

    fun hasIdentityChanged(): Boolean {
        return identityChanged
    }
}

interface OnAccountSelectedCallback {
    fun accountChosen(previousAccount: Account, account: Account)
}

interface OnIdentityUpdateFinishedCallback {
    fun onFinished(relatedMessageReference: MessageReference?, draftProperties: Map<IdentityField?, String?>)
}
