package com.fsck.k9.helper;

import android.net.Uri;

import com.fsck.k9.mail.Address;

import java.util.ArrayList;
import java.util.List;

public class MailTo {

    static public final String MAILTO_SCHEME = "mailto";

    private final Address[] toAddresses;
    private final Address[] ccAddresses;
    private final Address[] bccAddresses;
    private final String subject;
    private final String body;


    // Well known headers
    static private final String TO = "to";
    static private final String BODY = "body";
    static private final String CC = "cc";
    static private final String BCC = "bcc";
    static private final String SUBJECT = "subject";

    public static boolean isMailTo(Uri uri) {
        if (uri == null) {
            return false;
        }

        return MAILTO_SCHEME.equals(uri.getScheme());
    }

    public static MailTo parse(Uri uri) throws NullPointerException, IllegalArgumentException {
        if (uri == null || uri.toString() == null) {
            throw new NullPointerException();
        }

        if (!isMailTo(uri)) {
            throw new IllegalArgumentException("Not a mail to scheme");
        }

        String schemaSpecific = uri.getSchemeSpecificPart();
        int end = schemaSpecific.indexOf('?');
        if (end == -1) {
            end = schemaSpecific.length();
        }

        CaseInsensitiveParamWrapper caseInsensitiveParamWrapper = new CaseInsensitiveParamWrapper(
                Uri.parse("foo://bar?" + uri.getEncodedQuery()));

        // Extract the recipient's email address from the mailto URI if there's one.
        String recipient = Uri.decode(schemaSpecific.substring(0, end));

        // Read additional recipients from the "to" parameter.
        List<String> toList = caseInsensitiveParamWrapper.getQueryParameters(TO);
        if (recipient.length() != 0) {
            toList = new ArrayList<String>(toList);
            toList.add(0, recipient);
        }

        List<String> ccList = caseInsensitiveParamWrapper.getQueryParameters(CC);

        List<String> bccList = caseInsensitiveParamWrapper.getQueryParameters(BCC);

        List<String> subjectList = caseInsensitiveParamWrapper.getQueryParameters(SUBJECT);
        String subject = null;
        if (!subjectList.isEmpty()) {
            subject = subjectList.get(0);
        }

        List<String> bodyList = caseInsensitiveParamWrapper.getQueryParameters(BODY);
        String body = null;
        if (!bodyList.isEmpty()) {
            body = bodyList.get(0);
        }

        return new MailTo(listToCommaSeperatedString(toList), listToCommaSeperatedString(ccList), listToCommaSeperatedString(bccList), subject, body);
    }

    private MailTo(String toAddresses, String ccAddresses, String bccAddresses, String subject, String body) {
        this.toAddresses = Address.parseUnencoded(toAddresses);
        this.ccAddresses = Address.parseUnencoded(ccAddresses);
        this.bccAddresses = Address.parseUnencoded(bccAddresses);
        this.subject = subject;
        this.body = body;
    }

    private static String listToCommaSeperatedString(List<String> listStr) {
        StringBuilder strBuilder = new StringBuilder();
        String sep = "";
        for (String str : listStr) {
            strBuilder.append(sep).append(str);
            sep = ",";
        }

        return strBuilder.toString();
    }

    static class CaseInsensitiveParamWrapper {
        private final Uri uri;

        public CaseInsensitiveParamWrapper(Uri uri) {
            this.uri = uri;
        }

        public List<String> getQueryParameters(String key) {
            final List<String> params = new ArrayList<String>();
            for (String paramName : uri.getQueryParameterNames()) {
                if (paramName.equalsIgnoreCase(key)) {
                    params.addAll(uri.getQueryParameters(paramName));
                }
            }
            return params;
        }
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
}
