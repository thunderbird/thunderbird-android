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
 * This filter accepts <code>File</code>s that can be read.
 * <p>
 * Example, showing how to print out a list of the 
 * current directory's <i>readable</i> files:
 *
 * <pre>
 * File dir = new File(".");
 * String[] files = dir.list( CanReadFileFilter.CAN_READ );
 * for ( int i = 0; i &lt; files.length; i++ ) {
 *     System.out.println(files[i]);
 * }
 * </pre>
 *
 * <p>
 * Example, showing how to print out a list of the 
 * current directory's <i>un-readable</i> files:
 *
 * <pre>
 * File dir = new File(".");
 * String[] files = dir.list( CanReadFileFilter.CANNOT_READ );
 * for ( int i = 0; i &lt; files.length; i++ ) {
 *     System.out.println(files[i]);
 * }
 * </pre>
 *
 * <p>
 * Example, showing how to print out a list of the 
 * current directory's <i>read-only</i> files:
 *
 * <pre>
 * File dir = new File(".");
 * String[] files = dir.list( CanReadFileFilter.READ_ONLY );
 * for ( int i = 0; i &lt; files.length; i++ ) {
 *     System.out.println(files[i]);
 * }
 * </pre>
 *
 * @since Commons IO 1.3
 * @version $Revision: 587916 $
 */
public class CanReadFileFilter extends AbstractFileFilter implements Serializable {
    
    /** Singleton instance of <i>readable</i> filter */
    public static final IOFileFilter CAN_READ = new CanReadFileFilter();

    /** Singleton instance of not <i>readable</i> filter */
    public static final IOFileFilter CANNOT_READ = new NotFileFilter(CAN_READ);
    
    /** Singleton instance of <i>read-only</i> filter */
    public static final IOFileFilter READ_ONLY = new AndFileFilter(CAN_READ,
                                                CanWriteFileFilter.CANNOT_WRITE);
    
    /**
     * Restrictive consructor.
     */
    protected CanReadFileFilter() {
    }
    
    /**
     * Checks to see if the file can be read.
     * 
     * @param file  the File to check.
     * @return <code>true</code> if the file can be
     *  read, otherwise <code>false</code>.
     */
    public boolean accept(File file) {
        return file.canRead();
    }
    
}
