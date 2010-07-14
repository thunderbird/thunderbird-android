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

package org.apache.james.mime4j;

import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;

/**
 * InputStream which converts <code>\r</code>
 * bytes not followed by <code>\n</code> and <code>\n</code> not 
 * preceded by <code>\r</code> to <code>\r\n</code>.
 *
 * 
 * @version $Id: EOLConvertingInputStream.java,v 1.4 2004/11/29 13:15:42 ntherning Exp $
 */
public class EOLConvertingInputStream extends InputStream {
    /** Converts single '\r' to '\r\n' */
    public static final int CONVERT_CR   = 1;
    /** Converts single '\n' to '\r\n' */
    public static final int CONVERT_LF   = 2;
    /** Converts single '\r' and '\n' to '\r\n' */
    public static final int CONVERT_BOTH = 3;
    
    private PushbackInputStream in = null;
    private int previous = 0;
    private int flags = CONVERT_BOTH;
    
    /**
     * Creates a new <code>EOLConvertingInputStream</code>
     * instance converting bytes in the given <code>InputStream</code>.
     * The flag <code>CONVERT_BOTH</code> is the default.
     * 
     * @param in the <code>InputStream</code> to read from.
     */
    public EOLConvertingInputStream(InputStream in) {
        this(in, CONVERT_BOTH);
    }
    /**
     * Creates a new <code>EOLConvertingInputStream</code>
     * instance converting bytes in the given <code>InputStream</code>.
     * 
     * @param in the <code>InputStream</code> to read from.
     * @param flags one of <code>CONVERT_CR</code>, <code>CONVERT_LF</code> or
     *        <code>CONVERT_BOTH</code>.
     */
    public EOLConvertingInputStream(InputStream in, int flags) {
        super();
        
        this.in = new PushbackInputStream(in, 2);
        this.flags = flags;
    }

    /**
     * Closes the underlying stream.
     * 
     * @throws IOException on I/O errors.
     */
    public void close() throws IOException {
        in.close();
    }
    
    /**
     * @see java.io.InputStream#read()
     */
    public int read() throws IOException {
        int b = in.read();
        
        if (b == -1) {
            return -1;
        }
        
        if ((flags & CONVERT_CR) != 0 && b == '\r') {
            int c = in.read();
            if (c != -1) {
                in.unread(c);
            }
            if (c != '\n') {
                in.unread('\n');
            }
        } else if ((flags & CONVERT_LF) != 0 && b == '\n' && previous != '\r') {
            b = '\r';
            in.unread('\n');
        }
        
        previous = b;
        
        return b;
    }

}
