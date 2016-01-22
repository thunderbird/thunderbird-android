/*
 * Copyright (C) 2015 Dominik Schürmann <dominik@dominikschuermann.de>
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

interface IOpenPgpService2 {

    /**
     * see org.openintents.openpgp.util.OpenPgpApi for documentation
     */
    ParcelFileDescriptor createOutputPipe(in int pipeId);

    /**
     * see org.openintents.openpgp.util.OpenPgpApi for documentation
     */
    Intent execute(in Intent data, in ParcelFileDescriptor input, int pipeId);
}
