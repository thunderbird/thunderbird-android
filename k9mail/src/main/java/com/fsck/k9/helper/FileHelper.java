package com.fsck.k9.helper;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Locale;

import android.util.Log;

import com.fsck.k9.K9;


public class FileHelper {

    /**
     * Regular expression that represents characters we won't allow in file names.
     *
     * <p>
     * Allowed are:
     * <ul>
     *   <li>word characters (letters, digits, and underscores): {@code \w}</li>
     *   <li>spaces: {@code " "}</li>
     *   <li>special characters: {@code !}, {@code #}, {@code $}, {@code %}, {@code &}, {@code '},
     *       {@code (}, {@code )}, {@code -}, {@code @}, {@code ^}, {@code `}, <code>&#123;</code>,
     *       <code>&#125;</code>, {@code ~}, {@code .}, {@code ,}</li>
     * </ul></p>
     *
     * @see #sanitizeFilename(String)
     */
    private static final String INVALID_CHARACTERS = "[^\\w !#$%&'()\\-@\\^`{}~.,]";

    /**
     * Invalid characters in a file name are replaced by this character.
     *
     * @see #sanitizeFilename(String)
     */
    private static final String REPLACEMENT_CHARACTER = "_";


    /**
     * Creates a unique file in the given directory by appending a hyphen
     * and a number to the given filename.
     */
    public static File createUniqueFile(File directory, String filename) {
        File file = new File(directory, filename);
        if (!file.exists()) {
            return file;
        }
        // Get the extension of the file, if any.
        int index = filename.lastIndexOf('.');
        String format;
        if (index != -1) {
            String name = filename.substring(0, index);
            String extension = filename.substring(index);
            format = name + "-%d" + extension;
        } else {
            format = filename + "-%d";
        }
        for (int i = 2; i < Integer.MAX_VALUE; i++) {
            file = new File(directory, String.format(Locale.US, format, i));
            if (!file.exists()) {
                return file;
            }
        }
        return null;
    }

    public static void touchFile(final File parentDir, final String name) {
        final File file = new File(parentDir, name);
        try {
            if (!file.exists()) {
                if (!file.createNewFile()) {
                    Log.d(K9.LOG_TAG, "Unable to create file: " + file.getAbsolutePath());
                }
            } else {
                if (!file.setLastModified(System.currentTimeMillis())) {
                    Log.d(K9.LOG_TAG, "Unable to change last modification date: " + file.getAbsolutePath());
                }
            }
        } catch (Exception e) {
            Log.d(K9.LOG_TAG, "Unable to touch file: " + file.getAbsolutePath(), e);
        }
    }

    public static boolean move(final File from, final File to) {
        if (to.exists()) {
            if (!to.delete()) {
                Log.d(K9.LOG_TAG, "Unable to delete file: " + to.getAbsolutePath());
            }
        }

        if (!to.getParentFile().mkdirs()) {
            Log.d(K9.LOG_TAG, "Unable to make directories: " + to.getParentFile().getAbsolutePath());
        }

        try {
            FileInputStream in = new FileInputStream(from);
            try {
                FileOutputStream out = new FileOutputStream(to);
                try {
                    byte[] buffer = new byte[1024];
                    int count = -1;
                    while ((count = in.read(buffer)) > 0) {
                        out.write(buffer, 0, count);
                    }
                } finally {
                    out.close();
                }
            } finally {
                try { in.close(); } catch (Throwable ignore) {}
            }

            if (!from.delete()) {
                Log.d(K9.LOG_TAG, "Unable to delete file: " + from.getAbsolutePath());
            }
            return true;
        } catch (Exception e) {
            Log.w(K9.LOG_TAG, "cannot move " + from.getAbsolutePath() + " to " + to.getAbsolutePath(), e);
            return false;
        }

    }

    public static void moveRecursive(final File fromDir, final File toDir) {
        if (!fromDir.exists()) {
            return;
        }
        if (!fromDir.isDirectory()) {
            if (toDir.exists()) {
                if (!toDir.delete()) {
                    Log.w(K9.LOG_TAG, "cannot delete already existing file/directory " + toDir.getAbsolutePath());
                }
            }
            if (!fromDir.renameTo(toDir)) {
                Log.w(K9.LOG_TAG, "cannot rename " + fromDir.getAbsolutePath() + " to " + toDir.getAbsolutePath() + " - moving instead");
                move(fromDir, toDir);
            }
            return;
        }
        if (!toDir.exists() || !toDir.isDirectory()) {
            if (toDir.exists()) {
                if (!toDir.delete()) {
                    Log.d(K9.LOG_TAG, "Unable to delete file: " + toDir.getAbsolutePath());
                }
            }
            if (!toDir.mkdirs()) {
                Log.w(K9.LOG_TAG, "cannot create directory " + toDir.getAbsolutePath());
            }
        }
        File[] files = fromDir.listFiles();
        for (File file : files) {
            if (file.isDirectory()) {
                moveRecursive(file, new File(toDir, file.getName()));
                if (!file.delete()) {
                    Log.d(K9.LOG_TAG, "Unable to delete file: " + toDir.getAbsolutePath());
                }
            } else {
                File target = new File(toDir, file.getName());
                if (!file.renameTo(target)) {
                    Log.w(K9.LOG_TAG, "cannot rename " + file.getAbsolutePath() + " to " + target.getAbsolutePath() + " - moving instead");
                    move(file, target);
                }
            }
        }
        if (!fromDir.delete()) {
            Log.w(K9.LOG_TAG, "cannot delete " + fromDir.getAbsolutePath());
        }
    }

    /**
     * Replace characters we don't allow in file names with a replacement character.
     *
     * @param filename
     *         The original file name.
     *
     * @return The sanitized file name containing only allowed characters.
     */
    public static String sanitizeFilename(String filename) {
        return filename.replaceAll(INVALID_CHARACTERS, REPLACEMENT_CHARACTER);
    }
}
