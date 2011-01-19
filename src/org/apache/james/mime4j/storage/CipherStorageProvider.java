/****************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one   *
 * or more contributor license agreements.  See the NOTICE file *
 * distributed with this work for additional information        *
 * regarding copyright ownership.  The ASF licenses this file   *
 * to you under the Apache License, Version 2.0 (the            *
 * "License"); you may not use this file except in compliance   *
 * with the License.  You may obtain a copy of the License at   *
 *                                                              *
 *   http://www.apache.org/licenses/LICENSE-2.0                 *
 *                                                              *
 * Unless required by applicable law or agreed to in writing,   *
 * software distributed under the License is distributed on an  *
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY       *
 * KIND, either express or implied.  See the License for the    *
 * specific language governing permissions and limitations      *
 * under the License.                                           *
 ****************************************************************/

package org.apache.james.mime4j.storage;

import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.KeyGenerator;
import javax.crypto.spec.SecretKeySpec;

/**
 * A {@link StorageProvider} that transparently scrambles and unscrambles the
 * data stored by another <code>StorageProvider</code>.
 *
 * <p>
 * Example usage:
 *
 * <pre>
 * StorageProvider mistrusted = new TempFileStorageProvider();
 * StorageProvider enciphered = new CipherStorageProvider(mistrusted);
 * StorageProvider provider = new ThresholdStorageProvider(enciphered);
 * DefaultStorageProvider.setInstance(provider);
 * </pre>
 */
public class CipherStorageProvider extends AbstractStorageProvider {

    private final StorageProvider backend;
    private final String algorithm;
    private final KeyGenerator keygen;

    /**
     * Creates a new <code>CipherStorageProvider</code> for the given back-end
     * using the Blowfish cipher algorithm.
     *
     * @param backend
     *            back-end storage strategy to encrypt.
     */
    public CipherStorageProvider(StorageProvider backend) {
        this(backend, "Blowfish");
    }

    /**
     * Creates a new <code>CipherStorageProvider</code> for the given back-end
     * and cipher algorithm.
     *
     * @param backend
     *            back-end storage strategy to encrypt.
     * @param algorithm
     *            the name of the symmetric block cipher algorithm such as
     *            "Blowfish", "AES" or "RC2".
     */
    public CipherStorageProvider(StorageProvider backend, String algorithm) {
        if (backend == null)
            throw new IllegalArgumentException();

        try {
            this.backend = backend;
            this.algorithm = algorithm;
            this.keygen = KeyGenerator.getInstance(algorithm);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public StorageOutputStream createStorageOutputStream() throws IOException {
        SecretKeySpec skeySpec = getSecretKeySpec();

        return new CipherStorageOutputStream(backend
                .createStorageOutputStream(), algorithm, skeySpec);
    }

    private SecretKeySpec getSecretKeySpec() {
        byte[] raw = keygen.generateKey().getEncoded();
        return new SecretKeySpec(raw, algorithm);
    }

    private static final class CipherStorageOutputStream extends
            StorageOutputStream {
        private final StorageOutputStream storageOut;
        private final String algorithm;
        private final SecretKeySpec skeySpec;
        private final CipherOutputStream cipherOut;

        public CipherStorageOutputStream(StorageOutputStream out,
                String algorithm, SecretKeySpec skeySpec) throws IOException {
            try {
                this.storageOut = out;
                this.algorithm = algorithm;
                this.skeySpec = skeySpec;

                Cipher cipher = Cipher.getInstance(algorithm);
                cipher.init(Cipher.ENCRYPT_MODE, skeySpec);

                this.cipherOut = new CipherOutputStream(out, cipher);
            } catch (GeneralSecurityException e) {
                throw (IOException) new IOException().initCause(e);
            }
        }

        @Override
        public void close() throws IOException {
            super.close();
            cipherOut.close();
        }

        @Override
        protected void write0(byte[] buffer, int offset, int length)
                throws IOException {
            cipherOut.write(buffer, offset, length);
        }

        @Override
        protected Storage toStorage0() throws IOException {
            // cipherOut has already been closed because toStorage calls close
            Storage encrypted = storageOut.toStorage();
            return new CipherStorage(encrypted, algorithm, skeySpec);
        }
    }

    private static final class CipherStorage implements Storage {
        private Storage encrypted;
        private final String algorithm;
        private final SecretKeySpec skeySpec;

        public CipherStorage(Storage encrypted, String algorithm,
                SecretKeySpec skeySpec) {
            this.encrypted = encrypted;
            this.algorithm = algorithm;
            this.skeySpec = skeySpec;
        }

        public void delete() {
            if (encrypted != null) {
                encrypted.delete();
                encrypted = null;
            }
        }

        public InputStream getInputStream() throws IOException {
            if (encrypted == null)
                throw new IllegalStateException("storage has been deleted");

            try {
                Cipher cipher = Cipher.getInstance(algorithm);
                cipher.init(Cipher.DECRYPT_MODE, skeySpec);

                InputStream in = encrypted.getInputStream();
                return new CipherInputStream(in, cipher);
            } catch (GeneralSecurityException e) {
                throw (IOException) new IOException().initCause(e);
            }
        }
    }

}
