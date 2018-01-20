package com.fsck.k9.controller;


import com.fsck.k9.mail.Folder;


public class ControllerUtils {
    public static void closeFolder(Folder f) {
        if (f != null) {
            f.close();
        }
    }
}
