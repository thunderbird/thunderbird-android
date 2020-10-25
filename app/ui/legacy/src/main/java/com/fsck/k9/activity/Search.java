package com.fsck.k9.activity;


public class Search extends MessageList {
    @Override
    public void onStart() {
        getSearchStatusManager().setActive(true);
        super.onStart();
    }

    @Override
    public void onStop() {
        getSearchStatusManager().setActive(false);
        super.onStop();
    }

    @Override
    protected boolean isDrawerEnabled() {
        return false;
    }
}
