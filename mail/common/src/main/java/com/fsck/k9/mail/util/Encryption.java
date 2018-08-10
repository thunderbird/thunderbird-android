package com.fsck.k9.mail.util;

import android.app.Application;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.security.KeyPairGeneratorSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import android.util.Log;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.regex.Pattern;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.security.auth.x500.X500Principal;

/**
 * Created by csabavirag on 13/01/17.
 */

public final class Encryption extends Application {
    private static final String KEY_ALIAS = "k9_enc_key";
    private static final String AES_MODE = "AES/GCM/NoPadding";
    private static final String RSA_MODE =  "RSA/ECB/PKCS1Padding";
    private static final String AndroidKeyStore = "AndroidKeyStore";
    private static final String SHARED_PREFERENCE_NAME = "credentials";
    private static final String ENCRYPTED_KEY = "AES";
    private static Context mCtx;
    private static final SecureRandom random = new SecureRandom();


    /**
     * Constructor
     * @param ctx - receive the context from the main thread to have access to shared preferences and KeyStore
     */
    public Encryption(Context ctx) {
        mCtx = ctx;
    }

    /**
     * Public function to encode and encrypt a plain text string
     * @param string - plain text to encrypt
     * @return
     */
    public static String encode(String string)
    {
        if(!doesDeviceSupport())
            return string;
        String ret = "";
        if (string.length() > 0) {
            if(decode(string).length() > 0) {
                // Its already encoded so send the same string again.. rather doing double encryption.
                return string;
            }
        }
        if (string.length() > 0)
            ret = encrypt(string.getBytes(StandardCharsets.UTF_8));

        return ret;
    }

    /**
     * Public function to decode an encrypted string
     * @param string - The string must be a base64 encoded encrypted string
     * @return the decrypted value in plain text
     */
    public static String decode(String string)
    {
        if(!doesDeviceSupport())
            return string;
        byte[] b64;
        try {
            b64 = Base64.decode(string, Base64.DEFAULT);
        } catch (Exception e) {
            b64 = "".getBytes();
        }
        return new String(decrypt(b64));
    }

    /**
     * Private funtion to encrypt a plain text stored in byte array
     * @param input - plain text in byte array format
     * @return - the encrypted input in base64 encoded format
     */
    private static String encrypt(byte[] input) {
        Cipher c;
        String encryptedBase64Encoded = "";
        try {
            c = Cipher.getInstance(AES_MODE, "BC");
            byte[] ivBytes = generateIVBytes();

            //c.init(Cipher.ENCRYPT_MODE, getSecretKey(), new IvParameterSpec(ivBytes), random);
            GCMParameterSpec spec = new GCMParameterSpec(128, ivBytes);
            c.init(Cipher.ENCRYPT_MODE, getSecretKey(), spec, random);
            byte[] encryptedBytes = c.doFinal(input);
            byte[] encodedBytes = new byte[ivBytes.length + encryptedBytes.length];
            // Concatenate the Iv with the encrypted value. Iv will be extracted @ decryption
            System.arraycopy(ivBytes,0,encodedBytes,0,ivBytes.length);
            System.arraycopy(encryptedBytes,0,encodedBytes,ivBytes.length,encryptedBytes.length);

            encryptedBase64Encoded =  Base64.encodeToString(encodedBytes, Base64.DEFAULT);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return encryptedBase64Encoded;
    }


    /**
     * Private function to decrypt a byte array to plain text
     * @param encrypted - byte array structure: Iv bytes + Encrypted bytes
     * @return - plain text of the decrypted data
     */
    private static byte[] decrypt(byte[] encrypted) {
        Cipher c;
        byte[] decodedBytes = new byte[0];
        try {
            c = Cipher.getInstance(AES_MODE, "BC");
            //c.init(Cipher.DECRYPT_MODE, getSecretKey(), new IvParameterSpec(Arrays.copyOfRange(encrypted, 0, 12)), random);
            c.init(Cipher.DECRYPT_MODE, getSecretKey(), new GCMParameterSpec(128, Arrays.copyOfRange(encrypted, 0, 12)), random);
            decodedBytes = c.doFinal(Arrays.copyOfRange(encrypted, 12, encrypted.length));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return decodedBytes;
    }

    /**
     * Private helper to retrieve the secret AES key from SharedPreferences
     * AES key is protected by RSA private/public keypair stored in KeyStore
     * @return AES key
     * @throws Exception
     */
    private static Key getSecretKey() throws Exception{
        SharedPreferences pref = mCtx.getSharedPreferences(SHARED_PREFERENCE_NAME, mCtx.MODE_PRIVATE);
        String enryptedKeyB64 = pref.getString(ENCRYPTED_KEY, null);
        // need to check null, omitted here
        if (enryptedKeyB64 == null) {
            GenerateRSAKeyPair(); // If no AES key is available in SharedPrefs, generate RSA keypair
            GenerateAESKey();     // and generate the AES key and store in SharedPrefs
            enryptedKeyB64 = pref.getString(ENCRYPTED_KEY, null);
        }
        byte[] encryptedKey = Base64.decode(enryptedKeyB64, Base64.DEFAULT);
        byte[] key = rsaDecrypt(encryptedKey);
        return new SecretKeySpec(key, "AES");
    }

    /**
     * Private helper to encrypt AES key with RSA public key
     * @param secret - AES key to encrypt
     * @return - encrypted AES key in byte array format
     * @throws Exception
     */
    private static byte[] rsaEncrypt(byte[] secret) throws Exception{
        KeyStore keyStore = KeyStore.getInstance(AndroidKeyStore);
        keyStore.load(null);
        KeyStore.PrivateKeyEntry privateKeyEntry = (KeyStore.PrivateKeyEntry) keyStore.getEntry(KEY_ALIAS, null);
        // Encrypt the text
        Cipher inputCipher = getCipher();
        inputCipher.init(Cipher.ENCRYPT_MODE, privateKeyEntry.getCertificate().getPublicKey());

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        CipherOutputStream cipherOutputStream = new CipherOutputStream(outputStream, inputCipher);
        cipherOutputStream.write(secret);
        cipherOutputStream.close();

        byte[] vals = outputStream.toByteArray();
        return vals;
    }

    /**
     * Private helper to decrypt AES key with RSA private key
     * @param encrypted - encrypted AES key
     * @return - decrypted AES key in byte array format
     * @throws Exception
     */
    private static byte[]  rsaDecrypt(byte[] encrypted) throws Exception {
        KeyStore keyStore = KeyStore.getInstance(AndroidKeyStore);
        keyStore.load(null);
        KeyStore.PrivateKeyEntry privateKeyEntry = (KeyStore.PrivateKeyEntry)keyStore.getEntry(KEY_ALIAS, null);
        Cipher output = getCipher();
        output.init(Cipher.DECRYPT_MODE, privateKeyEntry.getPrivateKey());
        CipherInputStream cipherInputStream = new CipherInputStream(
                new ByteArrayInputStream(encrypted), output);
        ArrayList<Byte> values = new ArrayList<>();
        int nextByte;
        while ((nextByte = cipherInputStream.read()) != -1) {
            values.add((byte)nextByte);
        }

        byte[] bytes = new byte[values.size()];
        for(int i = 0; i < bytes.length; i++) {
            bytes[i] = values.get(i).byteValue();
        }
        return bytes;
    }

    /**
     * Private helper function to generate RSA keypair and store in KeyStore
     */
    private static void GenerateRSAKeyPair() {
        try {
            KeyStore keyStore = KeyStore.getInstance(AndroidKeyStore);
            keyStore.load(null);
// Generate the RSA key pairs
            if (!keyStore.containsAlias(KEY_ALIAS)) {
                // Generate a key pair for encryption
                Calendar start = Calendar.getInstance();
                Calendar end = Calendar.getInstance();
                end.add(Calendar.YEAR, 30);
                KeyPairGeneratorSpec spec = new      KeyPairGeneratorSpec.Builder(mCtx)
                        .setAlias(KEY_ALIAS)
                        .setSubject(new X500Principal("CN=" + KEY_ALIAS))
                        .setSerialNumber(BigInteger.TEN)
                        .setStartDate(start.getTime())
                        .setEndDate(end.getTime())
                        //.setEncryptionRequired() // Not forcing user to lock the screen.
                        .build();
                KeyPairGenerator kpg = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_RSA, AndroidKeyStore);
                kpg.initialize(spec);
                kpg.generateKeyPair();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Private helper function to generate AES key and encrypt with RSA finally store in SharedPrefs
     * @throws Exception
     */
    private static void GenerateAESKey() throws Exception {
        SharedPreferences pref = mCtx.getSharedPreferences(SHARED_PREFERENCE_NAME, mCtx.MODE_PRIVATE);
        String enryptedKeyB64 = pref.getString(ENCRYPTED_KEY, null);
        if (enryptedKeyB64 == null) {
            byte[] key = new byte[16];
            SecureRandom secureRandom = new SecureRandom();
            secureRandom.nextBytes(key);
            byte[] encryptedKey = rsaEncrypt(key);
            enryptedKeyB64 = Base64.encodeToString(encryptedKey, Base64.DEFAULT);
            SharedPreferences.Editor edit = pref.edit();
            edit.putString(ENCRYPTED_KEY, enryptedKeyB64);
            edit.commit();
        }
    }

    /**
     * Private helper to select the right cipher provider based on OS level
     * @return the right cipher instance
     */
    private static Cipher getCipher() {
        try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) { // below android m
                return Cipher.getInstance(RSA_MODE, "AndroidOpenSSL"); // error in android 6: InvalidKeyException: Need RSA private or public key
            }
            else { // android m and above
                return Cipher.getInstance(RSA_MODE, "AndroidKeyStoreBCWorkaround"); // error in android 5: NoSuchProviderException: Provider not available: AndroidKeyStoreBCWorkaround
            }
        } catch(Exception exception) {
            throw new RuntimeException("getCipher: Failed to get an instance of Cipher", exception);
        }
    }

    /**
     * Private helper funtion to generate Iv bytes for encrypting with AES
     * @return
     * @throws Exception
     */
    private static byte[] generateIVBytes() throws Exception {
        byte[] ivBytes = new byte[12];
        random.nextBytes(ivBytes);

        return ivBytes;
    }



    /* HEXDUMP for debugging */
    public static void HexDump(String desc,byte[] bytes) {
        int width=16;
        String h="";
        String a="";
        Log.d("Encryption", desc);
        for (int index = 0; index < bytes.length; index += width) {
            h=printHex(bytes, index, width);
            a=printAscii(bytes, index, width);
            Log.d("Encryption", h + ": " + a);
        }
    }

    private static String printHex(byte[] bytes, int offset, int width) {
        String s = "";
        for (int index = 0; index < width; index++) {
            if (index + offset < bytes.length) {
                System.out.printf("%02x ", bytes[index + offset]);
                s+=String.format("%02x ", bytes[index + offset]);
            } else {
                System.out.print("	");
                s+=" ";
            }
        }
        return s;
    }

    private static String printAscii(byte[] bytes, int index, int width) {
        String s="";
        if (index < bytes.length) {
            try {
                width = Math.min(width, bytes.length - index);
//            System.out.println(
                s = new String(bytes, index, width, "UTF-8").replaceAll("\r\n", " ").replaceAll("\n"," ");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
        return s;
    }

    // Check whether this device is compatible for this encryption methods.
    private static boolean doesDeviceSupport () {
        if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            // Support Marshmallow and above
            return false;
        }
        else {
            return true;
        }
    }

}
