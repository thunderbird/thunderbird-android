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
package org.apache.commons.io;

import java.io.File;
import java.io.IOException;

/**
 * Strategy for deleting files.
 * <p>
 * There is more than one way to delete a file.
 * You may want to limit access to certain directories, to only delete
 * directories if they are empty, or maybe to force deletion.
 * <p>
 * This class captures the strategy to use and is designed for user subclassing.
 *
 * @author Stephen Colebourne
 * @version $Id: FileDeleteStrategy.java 453903 2006-10-07 13:47:06Z scolebourne $
 * @since Commons IO 1.3
 */
public class FileDeleteStrategy {

    /**
     * The singleton instance for normal file deletion, which does not permit
     * the deletion of directories that are not empty.
     */
    public static final FileDeleteStrategy NORMAL = new FileDeleteStrategy("Normal");
    /**
     * The singleton instance for forced file deletion, which always deletes,
     * even if the file represents a non-empty directory.
     */
    public static final FileDeleteStrategy FORCE = new ForceFileDeleteStrategy();

    /** The name of the strategy. */
    private final String name;

    //-----------------------------------------------------------------------
    /**
     * Restricted constructor.
     *
     * @param name  the name by which the strategy is known
     */
    protected FileDeleteStrategy(String name) {
        this.name = name;
    }

    //-----------------------------------------------------------------------
    /**
     * Deletes the file object, which may be a file or a directory.
     * All <code>IOException</code>s are caught and false returned instead.
     * If the file does not exist or is null, true is returned.
     * <p>
     * Subclass writers should override {@link #doDelete(File)}, not this method.
     *
     * @param fileToDelete  the file to delete, null returns true
     * @return true if the file was deleted, or there was no such file
     */
    public boolean deleteQuietly(File fileToDelete) {
        if (fileToDelete == null || fileToDelete.exists() == false) {
            return true;
        }
        try {
            return doDelete(fileToDelete);
        } catch (IOException ex) {
            return false;
        }
    }

    /**
     * Deletes the file object, which may be a file or a directory.
     * If the file does not exist, the method just returns.
     * <p>
     * Subclass writers should override {@link #doDelete(File)}, not this method.
     *
     * @param fileToDelete  the file to delete, not null
     * @throws NullPointerException if the file is null
     * @throws IOException if an error occurs during file deletion
     */
    public void delete(File fileToDelete) throws IOException {
        if (fileToDelete.exists() && doDelete(fileToDelete) == false) {
            throw new IOException("Deletion failed: " + fileToDelete);
        }
    }

    /**
     * Actually deletes the file object, which may be a file or a directory.
     * <p>
     * This method is designed for subclasses to override.
     * The implementation may return either false or an <code>IOException</code>
     * when deletion fails. The {@link #delete(File)} and {@link #deleteQuietly(File)}
     * methods will handle either response appropriately.
     * A check has been made to ensure that the file will exist.
     * <p>
     * This implementation uses {@link File#delete()}.
     *
     * @param fileToDelete  the file to delete, exists, not null
     * @return true if the file was deleteds
     * @throws NullPointerException if the file is null
     * @throws IOException if an error occurs during file deletion
     */
    protected boolean doDelete(File fileToDelete) throws IOException {
        return fileToDelete.delete();
    }

    //-----------------------------------------------------------------------
    /**
     * Gets a string describing the delete strategy.
     *
     * @return a string describing the delete strategy
     */
    public String toString() {
        return "FileDeleteStrategy[" + name + "]";
    }

    //-----------------------------------------------------------------------
    /**
     * Force file deletion strategy.
     */
    static class ForceFileDeleteStrategy extends FileDeleteStrategy {
        /** Default Constructor */
        ForceFileDeleteStrategy() {
            super("Force");
        }

        /**
         * Deletes the file object.
         * <p>
         * This implementation uses <code>FileUtils.forceDelete() <code>
         * if the file exists.
         *
         * @param fileToDelete  the file to delete, not null
         * @return Always returns <code>true</code>
         * @throws NullPointerException if the file is null
         * @throws IOException if an error occurs during file deletion
         */
        protected boolean doDelete(File fileToDelete) throws IOException {
            FileUtils.forceDelete(fileToDelete);
            return true;
        }
    }

}
