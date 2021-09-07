package com.fsck.k9.ui.messageview;


/**
 * Interface for handling horizontal swipe actions.
 * The swipeToLeft parameter indicates if the swipe is towards the left or the right.
 */
public interface SwipeCatcher {
    void doSwipe(boolean swipeToLeft);
    boolean canSwipe(boolean swipeToLeft);
}
