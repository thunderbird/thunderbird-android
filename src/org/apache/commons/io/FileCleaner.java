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

/**
 * Keeps track of files awaiting deletion, and deletes them when an associated
 * marker object is reclaimed by the garbage collector.
 * <p>
 * This utility creates a background thread to handle file deletion.
 * Each file to be deleted is registered with a handler object.
 * When the handler object is garbage collected, the file is deleted.
 * <p>
 * In an environment with multiple class loaders (a servlet container, for
 * example), you should consider stopping the background thread if it is no
 * longer needed. This is done by invoking the method
 * {@link #exitWhenFinished}, typically in
 * {@link javax.servlet.ServletContextListener#contextDestroyed} or similar.
 *
 * @author Noel Bergman
 * @author Martin Cooper
 * @version $Id: FileCleaner.java 553012 2007-07-03 23:01:07Z ggregory $
 * @deprecated Use {@link FileCleaningTracker}
 */
public class FileCleaner {
    /**
     * The instance to use for the deprecated, static methods.
     */
    static final FileCleaningTracker theInstance = new FileCleaningTracker();

    //-----------------------------------------------------------------------
    /**
     * Track the specified file, using the provided marker, deleting the file
     * when the marker instance is garbage collected.
     * The {@link FileDeleteStrategy#NORMAL normal} deletion strategy will be used.
     *
     * @param file  the file to be tracked, not null
     * @param marker  the marker object used to track the file, not null
     * @throws NullPointerException if the file is null
     * @deprecated Use {@link FileCleaningTracker#track(File, Object)}.
     */
    public static void track(File file, Object marker) {
        theInstance.track(file, marker);
    }

    /**
     * Track the specified file, using the provided marker, deleting the file
     * when the marker instance is garbage collected.
     * The speified deletion strategy is used.
     *
     * @param file  the file to be tracked, not null
     * @param marker  the marker object used to track the file, not null
     * @param deleteStrategy  the strategy to delete the file, null means normal
     * @throws NullPointerException if the file is null
     * @deprecated Use {@link FileCleaningTracker#track(File, Object, FileDeleteStrategy)}.
     */
    public static void track(File file, Object marker, FileDeleteStrategy deleteStrategy) {
        theInstance.track(file, marker, deleteStrategy);
    }

    /**
     * Track the specified file, using the provided marker, deleting the file
     * when the marker instance is garbage collected.
     * The {@link FileDeleteStrategy#NORMAL normal} deletion strategy will be used.
     *
     * @param path  the full path to the file to be tracked, not null
     * @param marker  the marker object used to track the file, not null
     * @throws NullPointerException if the path is null
     * @deprecated Use {@link FileCleaningTracker#track(String, Object)}.
     */
    public static void track(String path, Object marker) {
        theInstance.track(path, marker);
    }

    /**
     * Track the specified file, using the provided marker, deleting the file
     * when the marker instance is garbage collected.
     * The speified deletion strategy is used.
     *
     * @param path  the full path to the file to be tracked, not null
     * @param marker  the marker object used to track the file, not null
     * @param deleteStrategy  the strategy to delete the file, null means normal
     * @throws NullPointerException if the path is null
     * @deprecated Use {@link FileCleaningTracker#track(String, Object, FileDeleteStrategy)}.
     */
    public static void track(String path, Object marker, FileDeleteStrategy deleteStrategy) {
        theInstance.track(path, marker, deleteStrategy);
    }

    //-----------------------------------------------------------------------
    /**
     * Retrieve the number of files currently being tracked, and therefore
     * awaiting deletion.
     *
     * @return the number of files being tracked
     * @deprecated Use {@link FileCleaningTracker#getTrackCount()}.
     */
    public static int getTrackCount() {
        return theInstance.getTrackCount();
    }

    /**
     * Call this method to cause the file cleaner thread to terminate when
     * there are no more objects being tracked for deletion.
     * <p>
     * In a simple environment, you don't need this method as the file cleaner
     * thread will simply exit when the JVM exits. In a more complex environment,
     * with multiple class loaders (such as an application server), you should be
     * aware that the file cleaner thread will continue running even if the class
     * loader it was started from terminates. This can consitute a memory leak.
     * <p>
     * For example, suppose that you have developed a web application, which
     * contains the commons-io jar file in your WEB-INF/lib directory. In other
     * words, the FileCleaner class is loaded through the class loader of your
     * web application. If the web application is terminated, but the servlet
     * container is still running, then the file cleaner thread will still exist,
     * posing a memory leak.
     * <p>
     * This method allows the thread to be terminated. Simply call this method
     * in the resource cleanup code, such as {@link javax.servlet.ServletContextListener#contextDestroyed}.
     * One called, no new objects can be tracked by the file cleaner.
     * @deprecated Use {@link FileCleaningTracker#exitWhenFinished()}.
     */
    public static synchronized void exitWhenFinished() {
        theInstance.exitWhenFinished();
    }

    /**
     * Returns the singleton instance, which is used by the deprecated, static methods.
     * This is mainly useful for code, which wants to support the new
     * {@link FileCleaningTracker} class while maintain compatibility with the
     * deprecated {@link FileCleaner}.
     * 
     * @return the singleton instance
     */
    public static FileCleaningTracker getInstance() {
        return theInstance;
    }
}
