/*
 * Copyright (C) 2014 Dominik Schürmann <dominik@dominikschuermann.de>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.sufficientlysecure.keychain.api;

public class OpenKeychainIntents {

    public static final String ENCRYPT = "org.sufficientlysecure.keychain.action.ENCRYPT";
    public static final String ENCRYPT_EXTRA_TEXT = "text"; // String
    public static final String ENCRYPT_ASCII_ARMOR = "ascii_armor"; // boolean

    public static final String DECRYPT = "org.sufficientlysecure.keychain.action.DECRYPT";
    public static final String DECRYPT_EXTRA_TEXT = "text"; // String

    public static final String IMPORT_KEY = "org.sufficientlysecure.keychain.action.IMPORT_KEY";
    public static final String IMPORT_KEY_EXTRA_KEY_BYTES = "key_bytes"; // byte[]

    public static final String IMPORT_KEY_FROM_KEYSERVER = "org.sufficientlysecure.keychain.action.IMPORT_KEY_FROM_KEYSERVER";
    public static final String IMPORT_KEY_FROM_KEYSERVER_QUERY = "query"; // String
    public static final String IMPORT_KEY_FROM_KEYSERVER_FINGERPRINT = "fingerprint"; // String

    public static final String IMPORT_KEY_FROM_QR_CODE = "org.sufficientlysecure.keychain.action.IMPORT_KEY_FROM_QR_CODE";

}
