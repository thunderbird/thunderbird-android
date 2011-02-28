package com.fsck.k9.preferences;

import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base64;



/**
 * Copied from:
 * http://www.androidsnippets.org/snippets/39/index.html
 * a page which had no licensing or copyright notice
 * and appeared to be intended for public use
 * package net.sf.andhsli.hotspotlogin;
 * Usage:
 * <pre>
 * String crypto = SimpleCrypto.encrypt(masterpassword, cleartext)
 * ...
 * String cleartext = SimpleCrypto.decrypt(masterpassword, crypto)
 * </pre>
 * @author ferenc.hechler
 */
public class SimpleCrypto {

    public static String encrypt(String seed, String cleartext, Base64 base64) throws Exception {
        byte[] rawKey = getRawKey(seed.getBytes());
        byte[] result = encrypt(rawKey, cleartext.getBytes());
        return new String(base64.encode(result));
    }

    public static String decrypt(String seed, String encrypted, Base64 base64) throws Exception {
        byte[] rawKey = getRawKey(seed.getBytes());
        byte[] enc = base64.decode(encrypted.getBytes());
        byte[] result = decrypt(rawKey, enc);
        return new String(result);
    }

    private static byte[] getRawKey(byte[] seed) throws Exception {
        KeyGenerator kgen = KeyGenerator.getInstance("AES");
        SecureRandom sr = SecureRandom.getInstance("SHA1PRNG");
        sr.setSeed(seed);
        kgen.init(128, sr); // 192 and 256 bits may not be available
        SecretKey skey = kgen.generateKey();
        byte[] raw = skey.getEncoded();
        return raw;
    }


    private static byte[] encrypt(byte[] raw, byte[] clear) throws Exception {
        SecretKeySpec skeySpec = new SecretKeySpec(raw, "AES");
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, skeySpec);
        byte[] encrypted = cipher.doFinal(clear);
        return encrypted;
    }

    private static byte[] decrypt(byte[] raw, byte[] encrypted) throws Exception {
        SecretKeySpec skeySpec = new SecretKeySpec(raw, "AES");
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.DECRYPT_MODE, skeySpec);
        byte[] decrypted = cipher.doFinal(encrypted);
        return decrypted;
    }

//
//    public static byte[] toByte(String hexString) {
//        int len = hexString.length()/2;
//        byte[] result = new byte[len];
//        for (int i = 0; i < len; i++)
//            result[i] = Integer.valueOf(hexString.substring(2*i, 2*i+2), 16).byteValue();
//        return result;
//    }
//
//    public static String toHex(byte[] buf) {
//        if (buf == null)
//            return "";
//        StringBuffer result = new StringBuffer(2*buf.length);
//        for (int i = 0; i < buf.length; i++) {
//            appendHex(result, buf[i]);
//        }
//        return result.toString();
//    }
//    private final static String HEX = "0123456789ABCDEF";
//    private static void appendHex(StringBuffer sb, byte b) {
//        sb.append(HEX.charAt((b>>4)&0x0f)).append(HEX.charAt(b&0x0f));
//    }
//
}

