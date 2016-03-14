package com.fsck.k9.mail.internet;

import android.net.MailTo;

import com.fsck.k9.mail.Address;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Intended to cover:
 *
 * RFC 2369
 * The Use of URLs as Meta-Syntax for Core Mail List Commands
 * and their Transport through Message Header Fields
 * https://www.ietf.org/rfc/rfc2369.txt
 *
 * This is the following fields:
 *
 * List-Help
 * List-Subscribe
 * List-Unsubscribe
 * List-Post
 * List-Owner
 * List-Archive
 *
 * Currently only provides a utility method for List-Post
 **/
public class ListHeaders {
    public static final String LIST_POST_HEADER = "List-Post";
    private static final Pattern mailtoContainerPattern = Pattern.compile("<(.+)>");

    public static Address[] parsePostAddress(String headerValue) {
        if (headerValue == null || headerValue.isEmpty()) {
            return new Address[]{};
        } else {
            Matcher m = mailtoContainerPattern.matcher(headerValue);
            if(m.find()) {
                Address[] addresses = { new Address(MailTo.parse(m.group(1)).getTo())};
                return addresses;
            } else {
                return new Address[]{};
            }
        }
    }
}
