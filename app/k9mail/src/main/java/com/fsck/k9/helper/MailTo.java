package com.fsck.k9.helper;


import java.util.ArrayList;
import java.util.List;

import android.net.Uri;

import com.fsck.k9.mail.Address;


public final class MailTo {
    private static final String MAILTO_SCHEME = "mailto";
    private static final String TO = "to";
    private static final String BODY = "body";
    private static final String CC = "cc";
    private static final String BCC = "bcc";
    private static final String SUBJECT = "subject";

    private static final Address[] EMPTY_ADDRESS_LIST = new Address[0];


    private final Address[] toAddresses;
    private final Address[] ccAddresses;
    private final Address[] bccAddresses;
    private final String subject;
    private final String body;


    public static boolean isMailTo(Uri uri) {
        return uri != null && MAILTO_SCHEME.equals(uri.getScheme());
    }

    public static MailTo parse(Uri uri) throws NullPointerException, IllegalArgumentException {
        if (uri == null || uri.toString() == null) {
            throw new NullPointerException("Argument 'uri' must not be null");
        }

        if (!isMailTo(uri)) {
            throw new IllegalArgumentException("Not a mailto scheme");
        }

        String schemaSpecific = uri.getSchemeSpecificPart();
        int end = schemaSpecific.indexOf('?');
        if (end == -1) {
            end = schemaSpecific.length();
        }

        CaseInsensitiveParamWrapper params =
                new CaseInsensitiveParamWrapper(Uri.parse("foo://bar?" + uri.getEncodedQuery()));

        // Extract the recipient's email address from the mailto URI if there's one.
        String recipient = Uri.decode(schemaSpecific.substring(0, end));

        List<String> toList = params.getQueryParameters(TO);
        if (recipient.length() != 0) {
            toList.add(0, recipient);
        }

        List<String> ccList = params.getQueryParameters(CC);
        List<String> bccList = params.getQueryParameters(BCC);

        Address[] toAddresses = toAddressArray(toList);
        Address[] ccAddresses = toAddressArray(ccList);
        Address[] bccAddresses = toAddressArray(bccList);

        String subject = getFirstParameterValue(params, SUBJECT);
        String body = getFirstParameterValue(params, BODY);

        return new MailTo(toAddresses, ccAddresses, bccAddresses, subject, body);
    }

    private static String getFirstParameterValue(CaseInsensitiveParamWrapper params, String paramName) {
        List<String> paramValues = params.getQueryParameters(paramName);

        return (paramValues.isEmpty()) ? null : paramValues.get(0);
    }

    private static Address[] toAddressArray(List<String> recipients) {
        if (recipients.isEmpty()) {
            return EMPTY_ADDRESS_LIST;
        }

        String addressList = toCommaSeparatedString(recipients);
        return Address.parse(addressList);
    }

    private static String toCommaSeparatedString(List<String> list) {
        StringBuilder stringBuilder = new StringBuilder();
        for (String item : list) {
            stringBuilder.append(item).append(',');
        }

        stringBuilder.setLength(stringBuilder.length() - 1);
        return stringBuilder.toString();
    }

    private MailTo(Address[] toAddresses, Address[] ccAddresses, Address[] bccAddresses, String subject, String body) {
        this.toAddresses = toAddresses;
        this.ccAddresses = ccAddresses;
        this.bccAddresses = bccAddresses;
        this.subject = subject;
        this.body = body;
    }

    public Address[] getTo() {
        return toAddresses;
    }

    public Address[] getCc() {
        return ccAddresses;
    }

    public Address[] getBcc() {
        return bccAddresses;
    }

    public String getSubject() {
        return subject;
    }

    public String getBody() {
        return body;
    }


    static class CaseInsensitiveParamWrapper {
        private final Uri uri;


        public CaseInsensitiveParamWrapper(Uri uri) {
            this.uri = uri;
        }

        public List<String> getQueryParameters(String key) {
            List<String> params = new ArrayList<String>();
            for (String paramName : uri.getQueryParameterNames()) {
                if (paramName.equalsIgnoreCase(key)) {
                    params.addAll(uri.getQueryParameters(paramName));
                }
            }

            return params;
        }
    }
}
