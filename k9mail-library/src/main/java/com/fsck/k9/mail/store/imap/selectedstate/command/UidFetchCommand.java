package com.fsck.k9.mail.store.imap.selectedstate.command;


import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

import com.fsck.k9.mail.BodyFactory;
import com.fsck.k9.mail.FetchProfile;
import com.fsck.k9.mail.K9MailLib;
import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.Part;
import com.fsck.k9.mail.store.imap.Commands;
import com.fsck.k9.mail.store.imap.FetchBodyCallback;
import com.fsck.k9.mail.store.imap.FetchPartCallback;
import com.fsck.k9.mail.store.imap.ImapConnection;
import com.fsck.k9.mail.store.imap.ImapResponse;
import com.fsck.k9.mail.store.imap.ImapResponseCallback;
import com.fsck.k9.mail.store.imap.ImapUtility;


public class UidFetchCommand extends SelectedStateCommand {
    private int maximumAutoDownloadMessageSize;
    private FetchProfile fetchProfile;
    private HashMap<String, Message> messageMap;
    private Part part;
    private BodyFactory bodyFactory;

    private UidFetchCommand() {
    }

    @Override
    String createCommandString() {
        StringBuilder builder = new StringBuilder(Commands.UID_FETCH).append(" ");
        builder.append(createCombinedIdString());
        addDataItems(builder);
        return builder.toString().trim();
    }

    public void send(ImapConnection connection) throws IOException, MessagingException {
        connection.sendCommand(createCommandString(), false);
    }

    public ImapResponse readResponse(ImapConnection connection) throws IOException {
        ImapResponseCallback callback = null;

        if (fetchProfile != null && messageMap != null) {
            if (fetchProfile.contains(FetchProfile.Item.BODY) || fetchProfile.contains(FetchProfile.Item.BODY_SANE)) {
                callback = new FetchBodyCallback(messageMap);
            }
        } else if (part != null) {
            callback = new FetchPartCallback(part, bodyFactory);
        }

        return connection.readResponse(callback);
    }

    private void addDataItems(StringBuilder builder) {
        if (fetchProfile != null && messageMap != null) {

            Set<String> fetchFields = new LinkedHashSet<>();
            fetchFields.add("UID");

            if (fetchProfile.contains(FetchProfile.Item.FLAGS)) {
                fetchFields.add("FLAGS");
            }

            if (fetchProfile.contains(FetchProfile.Item.ENVELOPE)) {
                fetchFields.add("INTERNALDATE");
                fetchFields.add("RFC822.SIZE");
                fetchFields.add("BODY.PEEK[HEADER.FIELDS (date subject from content-type to cc " +
                        "reply-to message-id references in-reply-to " + K9MailLib.IDENTITY_HEADER + ")]");
            }

            if (fetchProfile.contains(FetchProfile.Item.STRUCTURE)) {
                fetchFields.add("BODYSTRUCTURE");
            }

            if (fetchProfile.contains(FetchProfile.Item.BODY_SANE)) {
                if (maximumAutoDownloadMessageSize > 0) {
                    fetchFields.add(String.format(Locale.US, "BODY.PEEK[]<0.%d>", maximumAutoDownloadMessageSize));
                } else {
                    fetchFields.add("BODY.PEEK[]");
                }
            }

            if (fetchProfile.contains(FetchProfile.Item.BODY)) {
                fetchFields.add("BODY.PEEK[]");
            }

            String spaceSeparatedFetchFields = ImapUtility.join(" ", fetchFields);
            builder.append("(").append(spaceSeparatedFetchFields).append(")");
        } else if (part != null) {
            String partId = part.getServerExtra();

            String fetch;
            if ("TEXT".equalsIgnoreCase(partId)) {
                fetch = String.format(Locale.US, "BODY.PEEK[TEXT]<0.%d>", maximumAutoDownloadMessageSize);
            } else {
                fetch = String.format("BODY.PEEK[%s]", partId);
            }

            builder.append("(UID ").append(fetch).append(")");
        }
    }

    @Override
    Builder newBuilder() {
        return new Builder()
                .maximumAutoDownloadMessageSize(maximumAutoDownloadMessageSize)
                .messageParams(fetchProfile, messageMap)
                .partParams(part, bodyFactory);
    }

    public static class Builder extends SelectedStateCommand.Builder<UidFetchCommand, Builder> {

        public Builder maximumAutoDownloadMessageSize(int maximumAutoDownloadMessageSize) {
            command.maximumAutoDownloadMessageSize = maximumAutoDownloadMessageSize;
            return builder;
        }

        public Builder messageParams(FetchProfile fetchProfile, HashMap<String, Message> messageMap) {
            command.fetchProfile = fetchProfile;
            command.messageMap = messageMap;
            return builder;
        }

        public Builder partParams(Part part, BodyFactory bodyFactory) {
            command.part = part;
            command.bodyFactory = bodyFactory;
            return builder;
        }

        @Override
        UidFetchCommand createCommand() {
            return new UidFetchCommand();
        }

        @Override
        Builder createBuilder() {
            return this;
        }
    }
}
