/*
 * Copyright (C) 2013 Dominik Schürmann <dominik@dominikschuermann.de>
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

package org.openintents.openpgp;

import org.openintents.openpgp.OpenPgpSignatureResult;
import org.openintents.openpgp.OpenPgpError;

interface IOpenPgpCallback {
    
    /**
     * CryptoSignatureResult is only returned if the Callback was used from decryptAndVerify
     *
     */
    oneway void onSuccess(in byte[] outputBytes, in OpenPgpSignatureResult signatureResult);

    oneway void onError(in OpenPgpError error);
}