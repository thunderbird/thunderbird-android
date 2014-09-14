package com.fsck.k9.mail;

import java.security.MessageDigest;

import com.fsck.k9.mail.filter.Base64;
import com.fsck.k9.mail.filter.Hex;

public class Authentication {
    private static final String US_ASCII = "US-ASCII";

    /**
     * Computes the response for CRAM-MD5 authentication mechanism given the user credentials and
     * the server-provided nonce.
     *
     * @param username The username.
     * @param password The password.
     * @param b64Nonce The nonce as base64-encoded string.
     * @return The CRAM-MD5 response.
     *
     * @throws MessagingException If something went wrong.
     *
     * @see Authentication#computeCramMd5Bytes(String, String, byte[])
     */
    public static String computeCramMd5(String username, String password, String b64Nonce)
    throws MessagingException {

        try {
            byte[] b64NonceBytes = b64Nonce.getBytes(US_ASCII);
            byte[] b64CRAM = computeCramMd5Bytes(username, password, b64NonceBytes);
            return new String(b64CRAM, US_ASCII);
        } catch (MessagingException e) {
            throw e;
        } catch (Exception e) {
            throw new MessagingException("This shouldn't happen", e);
        }
    }

    /**
     * Computes the response for CRAM-MD5 authentication mechanism given the user credentials and
     * the server-provided nonce.
     *
     * @param username The username.
     * @param password The password.
     * @param b64Nonce The nonce as base64-encoded byte array.
     * @return The CRAM-MD5 response as byte array.
     *
     * @throws MessagingException If something went wrong.
     *
     * @see <a href="https://tools.ietf.org/html/rfc2195">RFC 2195</a>
     */
    public static byte[] computeCramMd5Bytes(String username, String password, byte[] b64Nonce)
    throws MessagingException {

        try {
            byte[] nonce = Base64.decodeBase64(b64Nonce);

            byte[] secretBytes = password.getBytes();
            MessageDigest md = MessageDigest.getInstance("MD5");
            if (secretBytes.length > 64) {
                secretBytes = md.digest(secretBytes);
            }

            byte[] ipad = new byte[64];
            byte[] opad = new byte[64];
            System.arraycopy(secretBytes, 0, ipad, 0, secretBytes.length);
            System.arraycopy(secretBytes, 0, opad, 0, secretBytes.length);
            for (int i = 0; i < ipad.length; i++) ipad[i] ^= 0x36;
            for (int i = 0; i < opad.length; i++) opad[i] ^= 0x5c;

            md.update(ipad);
            byte[] firstPass = md.digest(nonce);

            md.update(opad);
            byte[] result = md.digest(firstPass);

            String plainCRAM = username + " " + new String(Hex.encodeHex(result));
            byte[] b64CRAM = Base64.encodeBase64(plainCRAM.getBytes());

            return b64CRAM;

        } catch (Exception e) {
            throw new MessagingException("Something went wrong during CRAM-MD5 computation", e);
        }
    }
}
