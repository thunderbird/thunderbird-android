package com.fsck.k9.mail.store.exchange.adapter;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import com.fsck.k9.mail.Address;
import com.fsck.k9.mail.Flag;
import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.Message.RecipientType;
import com.fsck.k9.mail.internet.MimeMessage;
import com.fsck.k9.mail.store.exchange.Eas;

public class SearchParser extends Parser {
	
	private List<Message> mMessages = new ArrayList<Message>();
	
	public SearchParser(InputStream in) throws IOException {
		super(in);
	}
	
	@Override
	public boolean parse() throws IOException, MessagingException {
        boolean res = true;
        if (nextTag(START_DOCUMENT) != Tags.SEARCH_SEARCH) {
            throw new IOException();
        }

		if (nextTag(Tags.SEARCH_SEARCH) == Tags.SEARCH_STATUS) {
			int status = getValueInt();
			if (status != 1)
				return false;
		}
		
		if (nextTag(Tags.SEARCH_SEARCH) == Tags.SEARCH_RESPONSE) {
			if (nextTag(Tags.SEARCH_RESPONSE) == Tags.SEARCH_STORE) {
				if (nextTag(Tags.SEARCH_STORE) == Tags.SEARCH_STATUS) {
					int status = getValueInt();
					if (status != 1)
						return false;
					
					parseSearchResult();
				} else {
					return false;
				}
			}
		}
        return res;
	}
	
	private boolean parseSearchResult() throws IOException, MessagingException {
        while (nextTag(Tags.SEARCH_STORE) == Tags.SEARCH_RESULT) {
    		String longId;
    		Message msg = null;
    		
        	while (nextTag(Tags.SEARCH_RESULT) != END) {
                switch (tag) {
                    case Tags.SEARCH_LONG_ID:
                    	longId = getValue();
                    	break;
                    case Tags.SEARCH_PROPERTIES:
                    	msg = new MimeMessage();
                        addProperties(msg);
                        break;
                    default:
                        skipTag();
                }
        	}
        	
        	if (msg == null)
        		return false;
        	
        	mMessages.add(msg);
        }
		return true;
	}
	
    public void addProperties(Message msg) throws IOException, MessagingException {
//      ArrayList<Attachment> atts = new ArrayList<Attachment>();

      while (nextTag(Tags.SEARCH_PROPERTIES) != END) {
          switch (tag) {
//              case Tags.EMAIL_ATTACHMENTS:
//              case Tags.BASE_ATTACHMENTS: // BASE_ATTACHMENTS is used in EAS 12.0 and up
//                  attachmentsParser(msg);
//                  break;
              case Tags.EMAIL_TO:
                  msg.setRecipients(RecipientType.TO, Address.parse(getValue()));
                  break;
              case Tags.EMAIL_FROM:
                  Address[] froms = Address.parse(getValue());
                  if (froms != null && froms.length > 0) {
//                    msg.mDisplayName = froms[0].toFriendly();
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
//              	Date receivedDate = Utility.parseEmailDateTimeToMillis(getValue());
//                  msg.setInternalDate(receivedDate);
                  break;
              case Tags.EMAIL_SUBJECT:
                  msg.setSubject(getValue());
                  break;
              case Tags.EMAIL_READ:
              	msg.setFlag(Flag.SEEN, getValueInt() == 1);
                  break;
              case Tags.BASE_BODY:
                  bodyParser(msg);
                  break;
              case Tags.EMAIL_FLAG:
              	msg.setFlag(Flag.FLAGGED, flagParser());
                  break;
              case Tags.EMAIL_BODY:
                  String text = getValue();
//                  msg.setBody(new Body) = text;
                  break;
              case Tags.EMAIL_MESSAGE_CLASS:
                  String messageClass = getValue();
//                  if (messageClass.equals("IPM.Schedule.Meeting.Request")) {
//                      msg.mFlags |= Message.FLAG_INCOMING_MEETING_INVITE;
//                  } else if (messageClass.equals("IPM.Schedule.Meeting.Canceled")) {
//                      msg.mFlags |= Message.FLAG_INCOMING_MEETING_CANCEL;
//                  }
                  break;
//              case Tags.EMAIL_MEETING_REQUEST:
//                  meetingRequestParser(msg);
//                  break;
              default:
                  skipTag();
          }
      }

//      if (atts.size() > 0) {
//          msg.mAttachments = atts;
//      }
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
        if (bodyType.equals(Eas.BODY_PREFERENCE_HTML)) {
//            msg.mHtml = body;
        } else {
//            msg.mText = body;
        }
    }


	public Message[] getMessages() {
		return mMessages.toArray(new Message[0]);
	}
	
}
