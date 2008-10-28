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
import java.util.List;

import org.apache.commons.io.FilenameUtils;

/**
 * Filters files using the supplied wildcards.
 * <p>
 * This filter selects files, but not directories, based on one or more wildcards
 * and using case-sensitive comparison.
 * <p>
 * The wildcard matcher uses the characters '?' and '*' to represent a
 * single or multiple wildcard characters.
 * This is the same as often found on Dos/Unix command lines.
 * The extension check is case-sensitive.
 * See {@link FilenameUtils#wildcardMatch} for more information.
 * <p>
 * For example:
 * <pre>
 * File dir = new File(".");
 * FileFilter fileFilter = new WildcardFilter("*test*.java~*~");
 * File[] files = dir.listFiles(fileFilter);
 * for (int i = 0; i < files.length; i++) {
 *   System.out.println(files[i]);
 * }
 * </pre>
 *
 * @author Jason Anderson
 * @version $Revision: 606381 $ $Date: 2007-12-22 02:03:16 +0000 (Sat, 22 Dec 2007) $
 * @since Commons IO 1.1
 * @deprecated Use WilcardFileFilter. Deprecated as this class performs directory
 * filtering which it shouldn't do, but that can't be removed due to compatability.
 */
public class WildcardFilter extends AbstractFileFilter implements Serializable {

    /** The wildcards that will be used to match filenames. */
    private final String[] wildcards;

    /**
     * Construct a new case-sensitive wildcard filter for a single wildcard.
     *
     * @param wildcard  the wildcard to match
     * @throws IllegalArgumentException if the pattern is null
     */
    public WildcardFilter(String wildcard) {
        if (wildcard == null) {
            throw new IllegalArgumentException("The wildcard must not be null");
        }
        this.wildcards = new String[] { wildcard };
    }

    /**
     * Construct a new case-sensitive wildcard filter for an array of wildcards.
     *
     * @param wildcards  the array of wildcards to match
     * @throws IllegalArgumentException if the pattern array is null
     */
    public WildcardFilter(String[] wildcards) {
        if (wildcards == null) {
            throw new IllegalArgumentException("The wildcard array must not be null");
        }
        this.wildcards = wildcards;
    }

    /**
     * Construct a new case-sensitive wildcard filter for a list of wildcards.
     *
     * @param wildcards  the list of wildcards to match
     * @throws IllegalArgumentException if the pattern list is null
     * @throws ClassCastException if the list does not contain Strings
     */
    public WildcardFilter(List wildcards) {
        if (wildcards == null) {
            throw new IllegalArgumentException("The wildcard list must not be null");
        }
        this.wildcards = (String[]) wildcards.toArray(new String[wildcards.size()]);
    }

    //-----------------------------------------------------------------------
    /**
     * Checks to see if the filename matches one of the wildcards.
     *
     * @param dir  the file directory
     * @param name  the filename
     * @return true if the filename matches one of the wildcards
     */
    public boolean accept(File dir, String name) {
        if (dir != null && new File(dir, name).isDirectory()) {
            return false;
        }
        
        for (int i = 0; i < wildcards.length; i++) {
            if (FilenameUtils.wildcardMatch(name, wildcards[i])) {
                return true;
            }
        }
        
        return false;
    }

    /**
     * Checks to see if the filename matches one of the wildcards.
     *
     * @param file the file to check
     * @return true if the filename matches one of the wildcards
     */
    public boolean accept(File file) {
        if (file.isDirectory()) {
            return false;
        }
        
        for (int i = 0; i < wildcards.length; i++) {
            if (FilenameUtils.wildcardMatch(file.getName(), wildcards[i])) {
                return true;
            }
        }
        
        return false;
    }

}
