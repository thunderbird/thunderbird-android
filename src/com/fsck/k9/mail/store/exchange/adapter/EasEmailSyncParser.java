/**
 * 
 */
package com.fsck.k9.mail.store.exchange.adapter;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import android.webkit.MimeTypeMap;

import com.fsck.k9.Account;
import com.fsck.k9.mail.Address;
import com.fsck.k9.mail.Flag;
import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.Message.RecipientType;
import com.fsck.k9.mail.internet.MimeUtility;
import com.fsck.k9.mail.store.EasStore.EasFolder;
import com.fsck.k9.mail.store.EasStore.EasMessage;
import com.fsck.k9.mail.store.exchange.Eas;

public class EasEmailSyncParser extends AbstractSyncParser {

    /**
	 * 
	 */
	ArrayList<EasMessage> newEmails = new ArrayList<EasMessage>();
    ArrayList<String> deletedEmails = new ArrayList<String>();
    ArrayList<ServerChange> changedEmails = new ArrayList<ServerChange>();

    public EasEmailSyncParser(InputStream in, EasFolder folder, Account account) throws IOException {
        super(in, folder, account);
    }

    @Override
    public void wipe() {
//            mContentResolver.delete(Message.CONTENT_URI,
//                    Message.MAILBOX_KEY + "=" + mMailbox.mId, null);
//            mContentResolver.delete(Message.DELETED_CONTENT_URI,
//                    Message.MAILBOX_KEY + "=" + mMailbox.mId, null);
//            mContentResolver.delete(Message.UPDATED_CONTENT_URI,
//                    Message.MAILBOX_KEY + "=" + mMailbox.mId, null);
    }

    public void addData (EasMessage msg) throws IOException, MessagingException {
//            ArrayList<Attachment> atts = new ArrayList<Attachment>();

        while (nextTag(Tags.SYNC_APPLICATION_DATA) != END) {
            switch (tag) {
                case Tags.EMAIL_ATTACHMENTS:
                case Tags.BASE_ATTACHMENTS: // BASE_ATTACHMENTS is used in EAS 12.0 and up
                    attachmentsParser(msg);
                    break;
                case Tags.EMAIL_TO:
                    msg.setRecipients(RecipientType.TO, Address.parse(getValue()));
                    break;
                case Tags.EMAIL_FROM:
                    Address[] froms = Address.parse(getValue());
                    if (froms != null && froms.length > 0) {
//                          msg.mDisplayName = froms[0].toFriendly();
                    	msg.setFrom(froms[0]);
                    }
                    break;
                case Tags.EMAIL_CC:
                    msg.setRecipients(RecipientType.CC, Address.parse(getValue()));
                    break;
                case Tags.EMAIL_REPLY_TO:
                    msg.setReplyTo(Address.parse(getValue()));
                    break;
                case Tags.EMAIL_DATE_RECEIVED:
                	getValue();
//                    	Date receivedDate = Utility.parseEmailDateTimeToMillis(getValue());
//                        msg.setInternalDate(receivedDate);
                    break;
                case Tags.EMAIL_SUBJECT:
                    msg.setSubject(getValue());
                    break;
                case Tags.EMAIL_READ:
                	msg.setFlagInternal(Flag.SEEN, getValueInt() == 1);
                    break;
                case Tags.BASE_BODY:
                    bodyParser(msg);
                    break;
                case Tags.EMAIL_FLAG:
                	msg.setFlagInternal(Flag.FLAGGED, flagParser());
                    break;
                case Tags.EMAIL_BODY:
                    String body = getValue();
                    InputStream bodyStream = new ByteArrayInputStream(body.getBytes());

        			try {
        				msg.setBody(MimeUtility.decodeBody(bodyStream, null));
        			} catch (MessagingException e) {
        				throw new IOException(e);
        			}
                    break;
                case Tags.EMAIL_MESSAGE_CLASS:
                    String messageClass = getValue();
//                        if (messageClass.equals("IPM.Schedule.Meeting.Request")) {
//                            msg.mFlags |= Message.FLAG_INCOMING_MEETING_INVITE;
//                        } else if (messageClass.equals("IPM.Schedule.Meeting.Canceled")) {
//                            msg.mFlags |= Message.FLAG_INCOMING_MEETING_CANCEL;
//                        }
                    break;
                case Tags.EMAIL_MEETING_REQUEST:
                    meetingRequestParser(msg);
                    break;
                default:
                    skipTag();
            }
        }

//            if (atts.size() > 0) {
//                msg.mAttachments = atts;
//            }
    }

    /**
     * Set up the meetingInfo field in the message with various pieces of information gleaned
     * from MeetingRequest tags.  This information will be used later to generate an appropriate
     * reply email if the user chooses to respond
     * @param msg the Message being built
     * @throws IOException
     */
    private void meetingRequestParser(Message msg) throws IOException {
//            PackedString.Builder packedString = new PackedString.Builder();
        while (nextTag(Tags.EMAIL_MEETING_REQUEST) != END) {
            String value;
			switch (tag) {
                case Tags.EMAIL_DTSTAMP:
                    value = getValue();
//                        packedString.put(MeetingInfo.MEETING_DTSTAMP, value);
                    break;
                case Tags.EMAIL_START_TIME:
                    value = getValue();
//                        packedString.put(MeetingInfo.MEETING_DTSTART, value);
                    break;
                case Tags.EMAIL_END_TIME:
                    value = getValue();
//                        packedString.put(MeetingInfo.MEETING_DTEND, value);
                    break;
                case Tags.EMAIL_ORGANIZER:
                    value = getValue();
//                        packedString.put(MeetingInfo.MEETING_ORGANIZER_EMAIL, value);
                    break;
                case Tags.EMAIL_LOCATION:
                    value = getValue();
//                        packedString.put(MeetingInfo.MEETING_LOCATION, value);
                    break;
                case Tags.EMAIL_GLOBAL_OBJID:
                    value = getValue();
//                        packedString.put(MeetingInfo.MEETING_UID,
//                                CalendarUtilities.getUidFromGlobalObjId(value));
                    break;
                case Tags.EMAIL_CATEGORIES:
                    nullParser();
                    break;
                case Tags.EMAIL_RECURRENCES:
                    recurrencesParser();
                    break;
                default:
                    skipTag();
            }
        }
//            if (msg.mSubject != null) {
//                packedString.put(MeetingInfo.MEETING_TITLE, msg.mSubject);
//            }
//            msg.mMeetingInfo = packedString.toString();
    }

    private void nullParser() throws IOException {
        while (nextTag(Tags.EMAIL_CATEGORIES) != END) {
            skipTag();
        }
    }

    private void recurrencesParser() throws IOException {
        while (nextTag(Tags.EMAIL_RECURRENCES) != END) {
            switch (tag) {
                case Tags.EMAIL_RECURRENCE:
                    nullParser();
                    break;
                default:
                    skipTag();
            }
        }
    }

    private void addParser(ArrayList<EasMessage> emails) throws IOException, MessagingException {
    	EasMessage msg = new EasMessage(null, mFolder);
//            msg.mAccountKey = mAccount.mId;
//            msg.mMailboxKey = mMailbox.mId;
//            msg.mFlagLoaded = Message.FLAG_LOADED_COMPLETE;

        while (nextTag(Tags.SYNC_ADD) != END) {
            switch (tag) {
                case Tags.SYNC_SERVER_ID:
                	String serverId = getValue();
                    msg.setUid(serverId);
                    break;
                case Tags.SYNC_APPLICATION_DATA:
                    addData(msg);
                    break;
                default:
                    skipTag();
            }
        }
        emails.add(msg);
    }

    private void fetchParser(ArrayList<EasMessage> emails) throws IOException, MessagingException {
    	EasMessage msg = new EasMessage(null, mFolder);
//            msg.mAccountKey = mAccount.mId;
//            msg.mMailboxKey = mMailbox.mId;
//            msg.mFlagLoaded = Message.FLAG_LOADED_COMPLETE;

        while (nextTag(Tags.SYNC_FETCH) != END) {
            switch (tag) {
                case Tags.SYNC_SERVER_ID:
                	String serverId = getValue();
                    msg.setUid(serverId);
                    break;
                case Tags.SYNC_APPLICATION_DATA:
                    addData(msg);
                    break;
                default:
                    skipTag();
            }
        }
        emails.add(msg);
    }

    // For now, we only care about the "active" state
    private Boolean flagParser() throws IOException {
        Boolean state = false;
        while (nextTag(Tags.EMAIL_FLAG) != END) {
            switch (tag) {
                case Tags.EMAIL_FLAG_STATUS:
                    state = getValueInt() == 2;
                    break;
                default:
                    skipTag();
            }
        }
        return state;
    }

    private void bodyParser(Message msg) throws IOException {
        String bodyType = Eas.BODY_PREFERENCE_TEXT;
        String body = "";
        while (nextTag(Tags.EMAIL_BODY) != END) {
            switch (tag) {
                case Tags.BASE_TYPE:
                    bodyType = getValue();
                    break;
                case Tags.BASE_DATA:
                    body = getValue();
                    break;
                default:
                    skipTag();
            }
        }
        
        // We always ask for TEXT or HTML; there's no third option
//            if (bodyType.equals(Eas.BODY_PREFERENCE_HTML)) {
//                msg.mHtml = body;
//            } else {
//                msg.mText = body;
//            }

		try {
			InputStream bodyStream = new ByteArrayInputStream(body.getBytes());
			//String contentTransferEncoding;
//				contentTransferEncoding = msg.getHeader(
//				                                     MimeHeader.HEADER_CONTENT_TRANSFER_ENCODING)[0];
//				msg.setBody(MimeUtility.decodeBody(bodyStream, contentTransferEncoding));
			((EasMessage) msg).parse(bodyStream);
		} catch (MessagingException e) {
			throw new IOException(e);
		}
    }

    private void attachmentsParser(Message msg) throws IOException {
        while (nextTag(Tags.EMAIL_ATTACHMENTS) != END) {
            switch (tag) {
                case Tags.EMAIL_ATTACHMENT:
                case Tags.BASE_ATTACHMENT:  // BASE_ATTACHMENT is used in EAS 12.0 and up
                    attachmentParser(msg);
                    break;
                default:
                    skipTag();
            }
        }
    }

    private void attachmentParser(Message msg) throws IOException {
        String fileName = null;
        String length = null;
        String location = null;

        while (nextTag(Tags.EMAIL_ATTACHMENT) != END) {
            switch (tag) {
                // We handle both EAS 2.5 and 12.0+ attachments here
                case Tags.EMAIL_DISPLAY_NAME:
                case Tags.BASE_DISPLAY_NAME:
                    fileName = getValue();
                    break;
                case Tags.EMAIL_ATT_NAME:
                case Tags.BASE_FILE_REFERENCE:
                    location = getValue();
                    break;
                case Tags.EMAIL_ATT_SIZE:
                case Tags.BASE_ESTIMATED_DATA_SIZE:
                    length = getValue();
                    break;
                default:
                    skipTag();
            }
        }

        if ((fileName != null) && (length != null) && (location != null)) {
//                Attachment att = new Attachment();
//                att.mEncoding = "base64";
//                att.mSize = Long.parseLong(length);
//                att.mFileName = fileName;
//                att.mLocation = location;
//                att.mMimeType = getMimeTypeFromFileName(fileName);
//                atts.add(att);
//                msg.mFlagAttachment = true;
        }
    }

    /**
     * Try to determine a mime type from a file name, defaulting to application/x, where x
     * is either the extension or (if none) octet-stream
     * At the moment, this is somewhat lame, since many file types aren't recognized
     * @param fileName the file name to ponder
     * @return
     */
    // Note: The MimeTypeMap method currently uses a very limited set of mime types
    // A bug has been filed against this issue.
    public String getMimeTypeFromFileName(String fileName) {
        String mimeType;
        int lastDot = fileName.lastIndexOf('.');
        String extension = null;
        if ((lastDot > 0) && (lastDot < fileName.length() - 1)) {
            extension = fileName.substring(lastDot + 1).toLowerCase();
        }
        if (extension == null) {
            // A reasonable default for now.
            mimeType = "application/octet-stream";
        } else {
            mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
            if (mimeType == null) {
                mimeType = "application/" + extension;
            }
        }
        return mimeType;
    }

//        private Cursor getServerIdCursor(String serverId, String[] projection) {
//            mBindArguments[0] = serverId;
//            mBindArguments[1] = mMailboxIdAsString;
//            return mContentResolver.query(Message.CONTENT_URI, projection,
//                    WHERE_SERVER_ID_AND_MAILBOX_KEY, mBindArguments, null);
//        }

    /*package*/ void deleteParser(ArrayList<String> deletes, int entryTag) throws IOException {
        while (nextTag(entryTag) != END) {
            switch (tag) {
                case Tags.SYNC_SERVER_ID:
                    String serverId = getValue();
                    deletes.add(serverId);
                    break;
                default:
                    skipTag();
            }
        }
    }

    class ServerChange {
    	String serverId;
        Boolean read;
        Boolean flag;

        ServerChange(String _serverId, Boolean _read, Boolean _flag) {
            serverId = _serverId;
            read = _read;
            flag = _flag;
        }
    }

    /*package*/ void changeParser(ArrayList<ServerChange> changes) throws IOException {
        String serverId = null;
        while (nextTag(Tags.SYNC_CHANGE) != END) {
            switch (tag) {
                case Tags.SYNC_SERVER_ID:
                    serverId = getValue();
                    break;
                case Tags.SYNC_APPLICATION_DATA:
                    changeApplicationDataParser(changes, serverId);
                    break;
                default:
                    skipTag();
            }
        }
    }

    private void changeApplicationDataParser(ArrayList<ServerChange> changes, String serverId) throws IOException {
        Boolean read = null;
        Boolean flag = null;
        while (nextTag(Tags.SYNC_APPLICATION_DATA) != END) {
            switch (tag) {
                case Tags.EMAIL_READ:
                    read = getValueInt() == 1;
                    break;
                case Tags.EMAIL_FLAG:
                    flag = flagParser();
                    break;
                default:
                    skipTag();
            }
        }
        changes.add(new ServerChange(serverId, read, flag));
    }

    /* (non-Javadoc)
     * @see com.android.exchange.adapter.EasContentParser#commandsParser()
     */
    @Override
    public void commandsParser() throws IOException, MessagingException {
        while (nextTag(Tags.SYNC_COMMANDS) != END) {
            if (tag == Tags.SYNC_ADD) {
                addParser(newEmails);
//                this.emailSyncAdapter.incrementChangeCount();
            } else if (tag == Tags.SYNC_DELETE || tag == Tags.SYNC_SOFT_DELETE) {
                deleteParser(deletedEmails, tag);
//                this.emailSyncAdapter.incrementChangeCount();
            } else if (tag == Tags.SYNC_CHANGE) {
                changeParser(changedEmails);
//                this.emailSyncAdapter.incrementChangeCount();
            } else
                skipTag();
        }
    }

    @Override
    public void responsesParser() throws IOException, MessagingException {
        while (nextTag(Tags.SYNC_RESPONSES) != END) {
            if (tag == Tags.SYNC_FETCH) {
                fetchParser(newEmails);
//                this.emailSyncAdapter.incrementChangeCount();
//                } else if (tag == Tags.SYNC_ADD) {
//                    deleteParser(deletedEmails, tag);
//                    incrementChangeCount();
//                } else if (tag == Tags.SYNC_CHANGE) {
//                    changeParser(changedEmails);
//                    incrementChangeCount();
            } else
                skipTag();
        }
    }

    @Override
    public void commit() {
//            int notifyCount = 0;
//
//            // Use a batch operation to handle the changes
//            // TODO New mail notifications?  Who looks for these?
//            ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();
//            for (Message msg: newEmails) {
//                if (!msg.mFlagRead) {
//                    notifyCount++;
//                }
//                msg.addSaveOps(ops);
//            }
//            for (Long id : deletedEmails) {
//                ops.add(ContentProviderOperation.newDelete(
//                        ContentUris.withAppendedId(Message.CONTENT_URI, id)).build());
//                AttachmentProvider.deleteAllAttachmentFiles(mContext, mAccount.mId, id);
//            }
//            if (!changedEmails.isEmpty()) {
//                // Server wins in a conflict...
//                for (ServerChange change : changedEmails) {
//                     ContentValues cv = new ContentValues();
//                    if (change.read != null) {
//                        cv.put(MessageColumns.FLAG_READ, change.read);
//                    }
//                    if (change.flag != null) {
//                        cv.put(MessageColumns.FLAG_FAVORITE, change.flag);
//                    }
//                    ops.add(ContentProviderOperation.newUpdate(
//                            ContentUris.withAppendedId(Message.CONTENT_URI, change.id))
//                                .withValues(cv)
//                                .build());
//                }
//            }
//
//            // We only want to update the sync key here
//            ContentValues mailboxValues = new ContentValues();
//            mailboxValues.put(Mailbox.SYNC_KEY, mMailbox.mSyncKey);
//            ops.add(ContentProviderOperation.newUpdate(
//                    ContentUris.withAppendedId(Mailbox.CONTENT_URI, mMailbox.mId))
//                        .withValues(mailboxValues).build());
//
//            addCleanupOps(ops);
//
//            // No commits if we're stopped
//            synchronized (mService.getSynchronizer()) {
//                if (mService.isStopped()) return;
//                try {
//                    mContentResolver.applyBatch(EmailProvider.EMAIL_AUTHORITY, ops);
//                    userLog(mMailbox.mDisplayName, " SyncKey saved as: ", mMailbox.mSyncKey);
//                } catch (RemoteException e) {
//                    // There is nothing to be done here; fail by returning null
//                } catch (OperationApplicationException e) {
//                    // There is nothing to be done here; fail by returning null
//                }
//            }
//
//            if (notifyCount > 0) {
//                // Use the new atomic add URI in EmailProvider
//                // We could add this to the operations being done, but it's not strictly
//                // speaking necessary, as the previous batch preserves the integrity of the
//                // database, whereas this is purely for notification purposes, and is itself atomic
//                ContentValues cv = new ContentValues();
//                cv.put(EmailContent.FIELD_COLUMN_NAME, AccountColumns.NEW_MESSAGE_COUNT);
//                cv.put(EmailContent.ADD_COLUMN_NAME, notifyCount);
//                Uri uri = ContentUris.withAppendedId(Account.ADD_TO_FIELD_URI, mAccount.mId);
//                mContentResolver.update(uri, cv, null, null);
//                MailService.actionNotifyNewMessages(mContext, mAccount.mId);
//            }
    }


	public List<EasMessage> getMessages() throws MessagingException {
		List<EasMessage> messages = new ArrayList<EasMessage>();
		
		messages.addAll(newEmails);
		
		for (ServerChange srvChg : changedEmails) {
			EasMessage msg = new EasMessage(srvChg.serverId, mFolder);
			if (srvChg.read != null) {
				msg.setFlag(Flag.SEEN, srvChg.read);
			}
			if (srvChg.flag != null) {
				msg.setFlag(Flag.FLAGGED, srvChg.flag);
			}
			messages.add(msg);
		}
		
		for (String serverId : deletedEmails) {
			EasMessage msg = new EasMessage(serverId, mFolder);
			msg.setFlag(Flag.DELETED, true);
			messages.add(msg);
		}
		
		return messages;
	}

}