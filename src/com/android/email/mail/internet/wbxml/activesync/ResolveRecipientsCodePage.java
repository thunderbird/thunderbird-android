package com.android.email.mail.internet.wbxml.activesync;

import com.android.email.mail.internet.wbxml.CodePage;

/**
 * This class is the specific code page for ResolveRecipients in the ActiveSync protocol.
 * The code page number is 10.
 * 
 * @version 1.0
 * @author  Matthew Brace
 */
public class ResolveRecipientsCodePage extends CodePage {
    /**
     * Constructor for ResolveRecipientsCodePage.  Initializes all of the code page values.
     */
    public ResolveRecipientsCodePage() {
        /* Maps String to Token for the code page */
        codepageTokens.put("ResolveRecipients", 0x05);
        codepageTokens.put("Response", 0x06);
        codepageTokens.put("Status", 0x07);
        codepageTokens.put("Type", 0x08);
        codepageTokens.put("Recipient", 0x09);
        codepageTokens.put("DisplayName", 0x0a);
        codepageTokens.put("EmailAddress", 0x0b);
        codepageTokens.put("Certificates", 0x0c);
        codepageTokens.put("Certificate", 0x0d);
        codepageTokens.put("MiniCertificate", 0x0e);
        codepageTokens.put("Options", 0x0f);
        codepageTokens.put("To", 0x10);
        codepageTokens.put("CertificateRetrieval", 0x11);
        codepageTokens.put("RecipientCount", 0x12);
        codepageTokens.put("MaxCertificates", 0x13);
        codepageTokens.put("MaxAmbiguousRecipients", 0x14);
        codepageTokens.put("CertificateCount", 0x15);
         
        /* Maps token to string for the code page */
        for (String s : codepageTokens.keySet()) {
            codepageStrings.put(codepageTokens.get(s), s);
        }

        codePageIndex = 0x0a;
        codePageName = "ResolveRecipients";
    }
}
