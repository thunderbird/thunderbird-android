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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * A {@link java.io.FileFilter} providing conditional OR logic across a list of
 * file filters. This filter returns <code>true</code> if any filters in the
 * list return <code>true</code>. Otherwise, it returns <code>false</code>.
 * Checking of the file filter list stops when the first filter returns
 * <code>true</code>.
 *
 * @since Commons IO 1.0
 * @version $Revision: 606381 $ $Date: 2007-12-22 02:03:16 +0000 (Sat, 22 Dec 2007) $
 *
 * @author Steven Caswell
 */
public class OrFileFilter
        extends AbstractFileFilter
        implements ConditionalFileFilter, Serializable {

    /** The list of file filters. */
    private List fileFilters;

    /**
     * Constructs a new instance of <code>OrFileFilter</code>.
     *
     * @since Commons IO 1.1
     */
    public OrFileFilter() {
        this.fileFilters = new ArrayList();
    }

    /**
     * Constructs a new instance of <code>OrFileFilter</code>
     * with the specified filters.
     *
     * @param fileFilters  the file filters for this filter, copied, null ignored
     * @since Commons IO 1.1
     */
    public OrFileFilter(final List fileFilters) {
        if (fileFilters == null) {
            this.fileFilters = new ArrayList();
        } else {
            this.fileFilters = new ArrayList(fileFilters);
        }
    }

    /**
     * Constructs a new file filter that ORs the result of two other filters.
     * 
     * @param filter1  the first filter, must not be null
     * @param filter2  the second filter, must not be null
     * @throws IllegalArgumentException if either filter is null
     */
    public OrFileFilter(IOFileFilter filter1, IOFileFilter filter2) {
        if (filter1 == null || filter2 == null) {
            throw new IllegalArgumentException("The filters must not be null");
        }
        this.fileFilters = new ArrayList();
        addFileFilter(filter1);
        addFileFilter(filter2);
    }

    /**
     * {@inheritDoc}
     */
    public void addFileFilter(final IOFileFilter ioFileFilter) {
        this.fileFilters.add(ioFileFilter);
    }

    /**
     * {@inheritDoc}
     */
    public List getFileFilters() {
        return Collections.unmodifiableList(this.fileFilters);
    }

    /**
     * {@inheritDoc}
     */
    public boolean removeFileFilter(IOFileFilter ioFileFilter) {
        return this.fileFilters.remove(ioFileFilter);
    }

    /**
     * {@inheritDoc}
     */
    public void setFileFilters(final List fileFilters) {
        this.fileFilters = fileFilters;
    }

    /**
     * {@inheritDoc}
     */
    public boolean accept(final File file) {
        for (Iterator iter = this.fileFilters.iterator(); iter.hasNext();) {
            IOFileFilter fileFilter = (IOFileFilter) iter.next();
            if (fileFilter.accept(file)) {
                return true;
            }
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public boolean accept(final File file, final String name) {
        for (Iterator iter = this.fileFilters.iterator(); iter.hasNext();) {
            IOFileFilter fileFilter = (IOFileFilter) iter.next();
            if (fileFilter.accept(file, name)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Provide a String representaion of this file filter.
     *
     * @return a String representaion
     */
    public String toString() {
        StringBuffer buffer = new StringBuffer();
        buffer.append(super.toString());
        buffer.append("(");
        if (fileFilters != null) {
            for (int i = 0; i < fileFilters.size(); i++) {
                if (i > 0) {
                    buffer.append(",");
                }
                Object filter = fileFilters.get(i);
                buffer.append(filter == null ? "null" : filter.toString());
            }
        }
        buffer.append(")");
        return buffer.toString();
    }

}
