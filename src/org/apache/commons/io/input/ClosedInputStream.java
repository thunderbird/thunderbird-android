/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.commons.io.input;

import java.io.InputStream;

/**
 * Closed input stream. This stream returns -1 to all attempts to read
 * something from the stream.
 * <p>
 * Typically uses of this class include testing for corner cases in methods
 * that accept input streams and acting as a sentinel value instead of a
 * <code>null</code> input stream.
 *
 * @version $Id: ClosedInputStream.java 601751 2007-12-06 14:55:45Z niallp $
 * @since Commons IO 1.4
 */
public class ClosedInputStream extends InputStream {
    
    /**
     * A singleton.
     */
    public static final ClosedInputStream CLOSED_INPUT_STREAM = new ClosedInputStream();

    /**
     * Returns -1 to indicate that the stream is closed.
     *
     * @return always -1
     */
    public int read() {
        return -1;
    }

}
