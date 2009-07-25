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

import java.util.List;

/**
 * Defines operations for conditional file filters.
 *
 * @since Commons IO 1.1
 * @version $Revision: 437567 $ $Date: 2006-08-28 07:39:07 +0100 (Mon, 28 Aug 2006) $
 *
 * @author Steven Caswell
 */
public interface ConditionalFileFilter {

    /**
     * Adds the specified file filter to the list of file filters at the end of
     * the list.
     *
     * @param ioFileFilter the filter to be added
     * @since Commons IO 1.1
     */
    public void addFileFilter(IOFileFilter ioFileFilter);

    /**
     * Returns this conditional file filter's list of file filters.
     *
     * @return the file filter list
     * @since Commons IO 1.1
     */
    public List getFileFilters();

    /**
     * Removes the specified file filter.
     *
     * @param ioFileFilter filter to be removed
     * @return <code>true</code> if the filter was found in the list,
     * <code>false</code> otherwise
     * @since Commons IO 1.1
     */
    public boolean removeFileFilter(IOFileFilter ioFileFilter);

    /**
     * Sets the list of file filters, replacing any previously configured
     * file filters on this filter.
     *
     * @param fileFilters the list of filters
     * @since Commons IO 1.1
     */
    public void setFileFilters(List fileFilters);

}
