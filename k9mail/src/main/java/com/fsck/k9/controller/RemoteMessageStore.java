package com.fsck.k9.controller;


import com.fsck.k9.Account;
import com.fsck.k9.mail.Folder;


public interface RemoteMessageStore {
    // TODO: Nicer interface
    //       Instead of using Account pass in "remote store config", "sync config", "local mail store" (like LocalStore
    //       only with an interface/implementation optimized for sync; eventually this can replace LocalStore which does
    //       many things we don't need and does badly some of the things we do need), "folder id", "sync listener"
    // TODO: Add a way to cancel the sync process
    void sync(Account account, String folder, MessagingListener listener, Folder providedRemoteFolder);
}
