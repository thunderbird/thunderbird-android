package com.fsck.k9.message.signature;


import java.util.regex.Pattern;


public class TextSignatureRemover {
    private static final Pattern DASH_SIGNATURE_PLAIN = Pattern.compile("\r\n-- \r\n.*", Pattern.DOTALL);


    public static String stripSignature(String content) {
        if (DASH_SIGNATURE_PLAIN.matcher(content).find()) {
            content = DASH_SIGNATURE_PLAIN.matcher(content).replaceFirst("\r\n");
        }
        return content;
    }
}
