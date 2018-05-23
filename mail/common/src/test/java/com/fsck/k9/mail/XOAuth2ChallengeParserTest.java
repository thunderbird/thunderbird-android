package com.fsck.k9.mail;

import com.fsck.k9.mail.filter.Base64;


public class XOAuth2ChallengeParserTest {
    public static final String STATUS_400_RESPONSE = Base64.encode(
            "{\"status\":\"400\",\"schemes\":\"bearer mac\",\"scope\":\"https://mail.google.com/\"}");
    public static final String STATUS_401_RESPONSE = Base64.encode(
            "{\"status\":\"401\",\"schemes\":\"bearer mac\",\"scope\":\"https://mail.google.com/\"}");
    public static final String MISSING_STATUS_RESPONSE = Base64.encode(
            "{\"schemes\":\"bearer mac\",\"scope\":\"https://mail.google.com/\"}");
    public static final String INVALID_RESPONSE = Base64.encode(
            "{\"status\":\"401\",\"schemes\":\"bearer mac\",\"scope\":\"https://mail.google.com/\"");
}
