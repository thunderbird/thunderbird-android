package com.fsck.k9.mail.store.imap.response;

import java.util.List;

import com.fsck.k9.mail.K9MailLib;
import com.fsck.k9.mail.store.imap.ImapList;
import com.fsck.k9.mail.store.imap.ImapResponse;
import com.fsck.k9.mail.store.imap.ImapResponseParser;
import com.fsck.k9.mail.store.imap.command.ImapCommandFactory;
import timber.log.Timber;


public abstract class BaseResponse {

    private int messageCount;
    private int expungedCount;
    private long uidNext;
    private ImapCommandFactory commandFactory;

    BaseResponse(ImapCommandFactory commandFactory, List<ImapResponse> imapResponses) {
        this.commandFactory = commandFactory;
        messageCount = -1;
        expungedCount = 0;
        uidNext = -1L;
        handleUntaggedResponses(imapResponses);
        parseResponse(imapResponses);
    }

    abstract void parseResponse(List<ImapResponse> imapResponses);

    void combine(BaseResponse baseResponse) {
        if (baseResponse.messageCount != -1) {
            this.messageCount = baseResponse.messageCount;
        }
        this.expungedCount += baseResponse.expungedCount;
        if (baseResponse.uidNext != -1L) {
            this.uidNext = baseResponse.uidNext;
        }
    }

    public int getMessageCount(){
        return messageCount;
    }

    public int getExpungedCount() {
        return expungedCount;
    }

    public long getUidNext() {
        return uidNext;
    }

    private String getLogId() {
        return commandFactory.getLogId();
    }

    private void handleUntaggedResponses(List<ImapResponse> responses) {
        if (responses != null) {
            for (ImapResponse response : responses) {
                handleUntaggedResponses(response);
            }
        }
    }

    private void handleUntaggedResponses(ImapResponse response) {
        if (response.getTag() == null && response.size() > 1) {
            if (ImapResponseParser.equalsIgnoreCase(response.get(1), "EXISTS")) {
                messageCount = response.getNumber(0);
                if (K9MailLib.isDebug()) {
                    Timber.d("Got untagged EXISTS with value %d for %s", messageCount, getLogId());
                }
            }

            handlePossibleUidNext(response);

            if (ImapResponseParser.equalsIgnoreCase(response.get(1), "EXPUNGE")) {
                expungedCount++;
                if (K9MailLib.isDebug()) {
                    Timber.d("Got untagged EXPUNGE with messageCount %d for %s", messageCount, getLogId());
                }
            }
        }
    }

    private void handlePossibleUidNext(ImapResponse response) {
        if (ImapResponseParser.equalsIgnoreCase(response.get(0), "OK") && response.size() > 1) {
            Object bracketedObj = response.get(1);
            if (bracketedObj instanceof ImapList) {
                ImapList bracketed = (ImapList) bracketedObj;

                if (bracketed.size() > 1) {
                    Object keyObj = bracketed.get(0);
                    if (keyObj instanceof String) {
                        String key = (String) keyObj;
                        if ("UIDNEXT".equalsIgnoreCase(key)) {
                            uidNext = bracketed.getLong(1);
                            if (K9MailLib.isDebug()) {
                                Timber.d("Got UidNext = %s for %s", uidNext, getLogId());
                            }
                        }
                    }
                }
            }
        }
    }

}
