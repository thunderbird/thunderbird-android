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

package org.apache.james.mime4j.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * @version $Id: TempFile.java,v 1.3 2004/10/02 12:41:11 ntherning Exp $
 */
public interface TempFile {
    /**
     * Gets an <code>InputStream</code> to read bytes from this temporary file.
     * NOTE: The stream should NOT be wrapped in 
     * <code>BufferedInputStream</code> by the caller. If the implementing 
     * <code>TempFile</code> creates a <code>FileInputStream</code> or any
     * other stream which would benefit from being buffered it's the 
     * <code>TempFile</code>'s responsibility to wrap it.
     * 
     * @return the stream.
     * @throws IOException
     */
    InputStream getInputStream() throws IOException;
    
    /**
     * Gets an <code>OutputStream</code> to write bytes to this temporary file.
     * NOTE: The stream should NOT be wrapped in 
     * <code>BufferedOutputStream</code> by the caller. If the implementing 
     * <code>TempFile</code> creates a <code>FileOutputStream</code> or any
     * other stream which would benefit from being buffered it's the 
     * <code>TempFile</code>'s responsibility to wrap it.
     * 
     * @return the stream.
     * @throws IOException
     */
    OutputStream getOutputStream() throws IOException;
    
    /**
     * Returns the absolute path including file name of this 
     * <code>TempFile</code>. The path may be <code>null</code> if this is
     * an in-memory file.
     * 
     * @return the absolute path.
     */
    String getAbsolutePath();
    
    /**
     * Deletes this file as soon as possible.
     */
    void delete();
    
    /**
     * Determines if this is an in-memory file.
     * 
     * @return <code>true</code> if this file is currently in memory,
     *         <code>false</code> otherwise.
     */
    boolean isInMemory();
    
    /**
     * Gets the length of this temporary file.
     * 
     * @return the length.
     */
    long length();
}
