package com.fsck.k9.mail.internet;


import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.net.MailTo;

import com.fsck.k9.mail.Address;
import com.fsck.k9.mail.Message;


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
    private static final Pattern MAILTO_CONTAINER_PATTERN = Pattern.compile("<(mailto:.+)>");


    public static Address[] getListPostAddresses(Message message) {
        String[] headerValues = message.getHeader(LIST_POST_HEADER);
        if (headerValues.length < 1) {
            return new Address[0];
        }

        List<Address> listPostAddresses = new ArrayList<>();
        for (String headerValue : headerValues) {
            Address address = extractAddress(headerValue);
            if (address != null) {
                listPostAddresses.add(address);
            }
        }

        return listPostAddresses.toArray(new Address[listPostAddresses.size()]);
    }

    private static Address extractAddress(String headerValue) {
        if (headerValue == null || headerValue.isEmpty()) {
            return null;
        }

        Matcher matcher = MAILTO_CONTAINER_PATTERN.matcher(headerValue);
        if (!matcher.find()) {
            return null;
        }

        String mailToUri = matcher.group(1);
        String emailAddress = MailTo.parse(mailToUri).getTo();
        return new Address(emailAddress);
    }
}
