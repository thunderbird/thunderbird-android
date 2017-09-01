package com.fsck.k9.autocrypt;


import java.util.Collections;
import java.util.Date;

import android.content.Intent;

import com.fsck.k9.mail.Message;
import org.openintents.openpgp.AutocryptPeerUpdate;
import org.openintents.openpgp.util.OpenPgpApi;


public class AutocryptOperations {
    private final AutocryptHeaderParser autocryptHeaderParser;


    public static AutocryptOperations getInstance() {
        AutocryptHeaderParser autocryptHeaderParser = AutocryptHeaderParser.getInstance();
        return new AutocryptOperations(autocryptHeaderParser);
    }


    private AutocryptOperations(AutocryptHeaderParser autocryptHeaderParser) {
        this.autocryptHeaderParser = autocryptHeaderParser;
    }

    public boolean addAutocryptPeerUpdateToIntentIfPresent(Message currentMessage, Intent intent) {
        AutocryptHeader autocryptHeader = autocryptHeaderParser.getValidAutocryptHeader(currentMessage);
        if (autocryptHeader == null) {
            return false;
        }

        String messageFromAddress = currentMessage.getFrom()[0].getAddress();
        if (!autocryptHeader.addr.equalsIgnoreCase(messageFromAddress)) {
            return false;
        }

        Date messageDate = currentMessage.getSentDate();
        Date internalDate = currentMessage.getInternalDate();
        Date effectiveDate = messageDate.before(internalDate) ? messageDate : internalDate;

        AutocryptPeerUpdate data = AutocryptPeerUpdate.create(
                autocryptHeader.keyData, effectiveDate, autocryptHeader.isPreferEncryptMutual);
        intent.putExtra(OpenPgpApi.EXTRA_AUTOCRYPT_PEER_ID, messageFromAddress);
        intent.putExtra(OpenPgpApi.EXTRA_AUTOCRYPT_PEER_UPDATE, data);
        return true;
    }

    public boolean hasAutocryptHeader(Message currentMessage) {
        return currentMessage.getHeader(AutocryptHeader.AUTOCRYPT_HEADER).length > 0;
    }

    public void addAutocryptHeaderToMessage(Message message, byte[] keyData,
            String autocryptAddress, boolean preferEncryptMutual) {
        AutocryptHeader autocryptHeader = new AutocryptHeader(
                Collections.<String,String>emptyMap(), autocryptAddress, keyData, preferEncryptMutual);
        String rawAutocryptHeader = autocryptHeader.toRawHeaderString();

        message.addRawHeader(AutocryptHeader.AUTOCRYPT_HEADER, rawAutocryptHeader);
    }

}
