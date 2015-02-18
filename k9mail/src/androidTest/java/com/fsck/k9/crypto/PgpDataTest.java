package com.fsck.k9.crypto;

import android.support.test.runner.AndroidJUnit4;

import org.apache.commons.lang3.SerializationUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openintents.openpgp.OpenPgpSignatureResult;

import java.io.Serializable;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
/**
 * Test Class of PgpData
 */
public class PgpDataTest {

    /**
     * The object to test
     */
    private PgpData data;

    @Before
    /**
     * Creating a PgpData object
     */
    public void setUp(){
        data = new PgpData();
    }

    @Test
    /**
     * Testing initialization of PgpData
     */
    public void assertInit(){
        assertEquals(data.getEncryptionKeys(),null);
        assertEquals(data.getSignatureKeyId(),0);
        assertEquals(data.getSignatureUserId(),null);
        assertEquals(data.getSignatureSuccess(),false);
        assertEquals(data.getSignatureUnknown(),false);
        assertEquals(data.getEncryptedData(),null);
        assertEquals(data.getDecryptedData(),null);
    }

    @Test
    /**
     * Is it serializable ?
     */
    public void isSerializable(){
        Serializable copy = SerializationUtils.clone(data);
        assertEquals(data, copy);
    }

    @Test
    /**
     * Signature result setter test
     */
    public void setSignatureResultTest(){
        OpenPgpSignatureResult signature = new OpenPgpSignatureResult();
        signature.setKeyId(2014_02_17);
        data.setSignatureResult(signature);
        assertEquals(signature.getKeyId(),data.mSignatureResult.getKeyId());
    }

    @Test
    /**
     * Signature key setter test
     */
    public void setSignatureKeyIdTest(){
        long keyID = 2014_02_16L;
        data.setSignatureKeyId(keyID);
        assertEquals(keyID, data.mSignatureKeyId);
    }

    @Test
    /**
     * Encryption key setter test
     */
    public void setEncryptionKeysTest(){
        long [] enc = {2014_02_17, 2014_02_18, 2014_02_19};
        data.setEncryptionKeys(enc);
        assertEquals(enc[0], data.mEncryptionKeyIds[0]);
        assertEquals(enc[1], data.mEncryptionKeyIds[1]);
        assertEquals(enc[2],data.mEncryptionKeyIds[2]);
    }

    @Test
    /**
     * Has signature key method test
     */
    public void hasSignatureKeyTest(){
        long keyID = 2014_02_16L;
        assertFalse(data.hasSignatureKey());
        data.setSignatureKeyId(keyID);
        assertTrue(data.hasSignatureKey());
    }

    @Test
    /**
     * Has encryption key(s) method test
     */
    public void hasEncryptionKeysTest(){
        long [] enc = {2014_02_17,2014_02_18,2014_02_19};
        assertFalse(data.hasEncryptionKeys());
        data.setEncryptionKeys(enc);
        assertTrue(data.hasEncryptionKeys());
    }

    @Test
    /**
     * Encrypted data setter test
     */
    public void setEncryptedDataTest(){
        String enc = "LoremIpsum";
        data.setEncryptedData(enc);
        assertEquals(enc,data.mEncryptedData);
    }

    @Test
    /**
     * Decrypted data setter test
     */
    public void setDecryptedDataTest(){
        String dec = "LoremIpsum";
        data.setEncryptedData(dec);
        assertEquals(dec,data.mDecryptedData);
    }

    @Test
    /**
     * UserID signature setter test
     */
    public void setSignatureUserIdTest(){
        String id = "userId";
        data.setSignatureUserId(id);
        assertEquals(id,data.mSignatureUserId);
    }

    @Test
    /**
     * Signature success setter test
     */
    public void setSignatureSuccessTest(){
        data.setSignatureSuccess(true);
        assertTrue(data.mSignatureSuccess);
        data.setSignatureSuccess(false);
        assertFalse(data.mSignatureSuccess);
    }

    @Test
    /**
     * Signature unknown setter test
     */
    public void setSignatureUnknownTest(){
        data.setSignatureUnknown(true);
        assertTrue(data.mSignatureUnknown);
        data.setSignatureUnknown(false);
        assertFalse(data.mSignatureUnknown);
    }

}
