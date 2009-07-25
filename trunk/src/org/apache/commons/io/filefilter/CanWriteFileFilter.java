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
package org.apache.commons.io.filefilter;

import java.io.File;
import java.io.Serializable;

/**
 * This filter accepts <code>File</code>s that can be written to.
 * <p>
 * Example, showing how to print out a list of the
 * current directory's <i>writable</i> files:
 *
 * <pre>
 * File dir = new File(".");
 * String[] files = dir.list( CanWriteFileFilter.CAN_WRITE );
 * for ( int i = 0; i &lt; files.length; i++ ) {
 *     System.out.println(files[i]);
 * }
 * </pre>
 *
 * <p>
 * Example, showing how to print out a list of the
 * current directory's <i>un-writable</i> files:
 *
 * <pre>
 * File dir = new File(".");
 * String[] files = dir.list( CanWriteFileFilter.CANNOT_WRITE );
 * for ( int i = 0; i &lt; files.length; i++ ) {
 *     System.out.println(files[i]);
 * }
 * </pre>
 *
 * <p>
 * <b>N.B.</b> For read-only files, use 
 *    <code>CanReadFileFilter.READ_ONLY</code>.
 *
 * @since Commons IO 1.3
 * @version $Revision: 587916 $
 */
public class CanWriteFileFilter extends AbstractFileFilter implements Serializable {
    
    /** Singleton instance of <i>writable</i> filter */
    public static final IOFileFilter CAN_WRITE = new CanWriteFileFilter();

    /** Singleton instance of not <i>writable</i> filter */
    public static final IOFileFilter CANNOT_WRITE = new NotFileFilter(CAN_WRITE);

    /**
     * Restrictive consructor.
     */
    protected CanWriteFileFilter() {
    }
    
    /**
     * Checks to see if the file can be written to.
     * 
     * @param file  the File to check
     * @return <code>true</code> if the file can be
     *  written to, otherwise <code>false</code>.
     */
    public boolean accept(File file) {
        return file.canWrite();
    }
    
}
