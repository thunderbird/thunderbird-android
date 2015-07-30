package com.fsck.k9.mail.internet;

import com.fsck.k9.mail.Address;
import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.Part;

class JisSupport {
    public static final String SHIFT_JIS = "shift_jis";

    public static String getJisVariantFromMessage(Message message) throws MessagingException {
        if (message == null)
            return null;

        // If a receiver is known to use a JIS variant, the sender transfers the message after converting the
        // charset as a convention.
        String variant = getJisVariantFromReceivedHeaders(message);
        if (variant != null)
            return variant;

        // If a receiver is not known to use any JIS variants, the sender transfers the message without converting
        // the charset.
        variant = getJisVariantFromFromHeaders(message);
        if (variant != null)
            return variant;

        return getJisVariantFromMailerHeaders(message);
    }

    public static boolean isShiftJis(String charset) {
        return charset.length() > 17 && charset.startsWith("x-")
                && charset.endsWith("-shift_jis-2007");
    }

    public static String getJisVariantFromAddress(String address) {
        if (address == null)
            return null;
        if (isInDomain(address, "docomo.ne.jp") || isInDomain(address, "dwmail.jp") ||
                isInDomain(address, "pdx.ne.jp") || isInDomain(address, "willcom.com") ||
                isInDomain(address, "emnet.ne.jp") || isInDomain(address, "emobile.ne.jp"))
            return "docomo";
        else if (isInDomain(address, "softbank.ne.jp") || isInDomain(address, "vodafone.ne.jp") ||
                isInDomain(address, "disney.ne.jp") || isInDomain(address, "vertuclub.ne.jp"))
            return "softbank";
        else if (isInDomain(address, "ezweb.ne.jp") || isInDomain(address, "ido.ne.jp"))
            return "kddi";
        return null;
    }


    private static String getJisVariantFromMailerHeaders(Message message) throws MessagingException {
        String[] mailerHeaders = message.getHeader("X-Mailer");
        if (mailerHeaders.length == 0)
            return null;

        if (mailerHeaders[0].startsWith("iPhone Mail ") || mailerHeaders[0].startsWith("iPad Mail "))
            return "iphone";

        return null;
    }


    private static String getJisVariantFromReceivedHeaders(Part message) throws MessagingException {
        String[] receivedHeaders = message.getHeader("Received");
        if (receivedHeaders.length == 0)
            return null;

        for (String receivedHeader : receivedHeaders) {
            String address = getAddressFromReceivedHeader(receivedHeader);
            if (address == null)
                continue;
            String variant = getJisVariantFromAddress(address);
            if (variant != null)
                return variant;
        }
        return null;
    }

    private static String getAddressFromReceivedHeader(String receivedHeader) {
        // Not implemented yet!  Extract an address from the FOR clause of the given Received header.
        return null;
    }

    private static String getJisVariantFromFromHeaders(Message message) throws MessagingException {
        Address addresses[] = message.getFrom();
        if (addresses == null || addresses.length == 0)
            return null;

        return getJisVariantFromAddress(addresses[0].getAddress());
    }

    private static boolean isInDomain(String address, String domain) {
        int index = address.length() - domain.length() - 1;
        if (index < 0)
            return false;

        char c = address.charAt(index);
        if (c != '@' && c != '.')
            return false;

        return address.endsWith(domain);
    }
}
