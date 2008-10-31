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
package org.apache.commons.io.output;

import java.io.OutputStream;

/**
 * Proxy stream that prevents the underlying output stream from being closed.
 * <p>
 * This class is typically used in cases where an output stream needs to be
 * passed to a component that wants to explicitly close the stream even if
 * other components would still use the stream for output.
 *
 * @version $Id: CloseShieldOutputStream.java 587913 2007-10-24 15:47:30Z niallp $
 * @since Commons IO 1.4
 */
public class CloseShieldOutputStream extends ProxyOutputStream {

    /**
     * Creates a proxy that shields the given output stream from being
     * closed.
     *
     * @param out underlying output stream
     */
    public CloseShieldOutputStream(OutputStream out) {
        super(out);
    }

    /**
     * Replaces the underlying output stream with a {@link ClosedOutputStream}
     * sentinel. The original output stream will remain open, but this proxy
     * will appear closed.
     */
    public void close() {
        out = new ClosedOutputStream();
    }

}
