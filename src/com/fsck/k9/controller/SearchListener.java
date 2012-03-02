package com.fsck.k9.controller;

public interface SearchListener {
    public void searchStarted();
    public void searchFinished(int numResults);
}
