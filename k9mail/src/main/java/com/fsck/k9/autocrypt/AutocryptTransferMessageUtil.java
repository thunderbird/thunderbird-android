package com.fsck.k9.autocrypt;


import java.util.Date;

import android.content.res.Resources;

import com.fsck.k9.K9;
import com.fsck.k9.R;
import com.fsck.k9.mail.Address;
import com.fsck.k9.mail.Flag;
import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.Message.RecipientType;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.internet.MimeBodyPart;
import com.fsck.k9.mail.internet.MimeHeader;
import com.fsck.k9.mail.internet.MimeMessage;
import com.fsck.k9.mail.internet.MimeMessageHelper;
import com.fsck.k9.mail.internet.MimeMultipart;
import com.fsck.k9.mail.internet.TextBody;
import com.fsck.k9.mailstore.BinaryMemoryBody;


public class AutocryptTransferMessageUtil {
    public static Message createAutocryptTransferMessage(Resources resources, byte[] data, Address address) {
        try {
            String subjectText = resources.getString(R.string.ac_transfer_msg_subject);
            String messageText = resources.getString(R.string.ac_transfer_msg_body);

            MimeBodyPart textBodyPart = new MimeBodyPart(new TextBody(messageText));
            MimeBodyPart dataBodyPart = new MimeBodyPart(new BinaryMemoryBody(data, "7bit"));
            dataBodyPart.setHeader(MimeHeader.HEADER_CONTENT_TYPE, "application/autocrypt-setup");
            dataBodyPart.setHeader(MimeHeader.HEADER_CONTENT_DISPOSITION, "attachment; filename=\"autocrypt-setup-message\"");

            MimeMultipart messageBody = MimeMultipart.newInstance();
            messageBody.addBodyPart(textBodyPart);
            messageBody.addBodyPart(dataBodyPart);

            MimeMessage message = new MimeMessage();
            MimeMessageHelper.setBody(message, messageBody);

            Date nowDate = new Date();

            message.setFlag(Flag.X_DOWNLOADED_FULL, true);
            message.setSubject(subjectText);
            message.setHeader("Autocrypt-Setup-Message", "v1");
            message.setInternalDate(nowDate);
            message.addSentDate(nowDate, K9.hideTimeZone());
            message.setFrom(address);
            message.setRecipients(RecipientType.TO, new Address[] { address });

            return message;
        } catch (MessagingException e) {
            throw new AssertionError(e);
        }
    }
}
