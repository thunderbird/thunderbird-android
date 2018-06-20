package com.fsck.k9.activity;


import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;


public class MessageReferenceHelper {
    public static List<MessageReference> toMessageReferenceList(List<String> messageReferenceStrings) {
        List<MessageReference> messageReferences = new ArrayList<>(messageReferenceStrings.size());
        for (String messageReferenceString : messageReferenceStrings) {
            MessageReference messageReference = MessageReference.parse(messageReferenceString);
            if (messageReference != null) {
                messageReferences.add(messageReference);
            } else {
                Timber.w("Invalid message reference: %s", messageReferenceString);
            }
        }

        return messageReferences;
    }

    public static ArrayList<String> toMessageReferenceStringList(List<MessageReference> messageReferences) {
        ArrayList<String> messageReferenceStrings = new ArrayList<>(messageReferences.size());
        for (MessageReference messageReference : messageReferences) {
            String messageReferenceString = messageReference.toIdentityString();
            messageReferenceStrings.add(messageReferenceString);
        }

        return messageReferenceStrings;
    }
}
