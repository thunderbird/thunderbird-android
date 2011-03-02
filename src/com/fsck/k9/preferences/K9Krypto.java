package com.fsck.k9.preferences;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base64;

public class K9Krypto {
    final Base64 mBase64;
    final Cipher mCipher;

    private final static String AES = "AES";
    private final static String SECURE_RANDOM_TYPE = "SHA1PRNG";

    public enum MODE {
        ENCRYPT(Cipher.ENCRYPT_MODE), DECRYPT(Cipher.DECRYPT_MODE);

        int mode;
        private MODE(int nMode) {
            mode = nMode;
        }
    }

    public K9Krypto(String key, MODE mode) throws NoSuchAlgorithmException, InvalidKeyException, NoSuchPaddingException {
        mBase64 = new Base64();
        KeyGenerator keyGenerator = KeyGenerator.getInstance(AES);
        SecureRandom secureRandom = SecureRandom.getInstance(SECURE_RANDOM_TYPE);
        secureRandom.setSeed(key.getBytes());
        keyGenerator.init(128, secureRandom);
        SecretKey secretKey = keyGenerator.generateKey();
        byte[] processedKey = secretKey.getEncoded();
        mCipher = setupCipher(mode.mode, processedKey);
    }

    public String encrypt(String plainText) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        byte[] encryptedText = mCipher.doFinal(plainText.getBytes());
        byte[] encryptedEncodedText = mBase64.encode(encryptedText);
        return new String(encryptedEncodedText);
    }

    public String decrypt(String encryptedEncodedText) throws IllegalBlockSizeException, BadPaddingException {
        byte[] encryptedText = mBase64.decode(encryptedEncodedText.getBytes());
        byte[] plainText = mCipher.doFinal(encryptedText);
        return new String(plainText);
    }

    private Cipher setupCipher(int mode, byte[] processedKey) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException {
        SecretKeySpec secretKeySpec = new SecretKeySpec(processedKey, AES);
        Cipher cipher = Cipher.getInstance(AES);
        cipher.init(mode, secretKeySpec);
        return cipher;
    }

}
