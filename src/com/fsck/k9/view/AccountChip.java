package com.fsck.k9.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import com.fsck.k9.K9;

public class AccountChip extends View {

    public AccountChip(Context context, AttributeSet attrs) {
        super(context, attrs);
        toggleVisibility(K9.showAccountColors());
    }

    private void toggleVisibility(boolean visible) {
        int visibility = visible ? View.VISIBLE : View.GONE;
        setVisibility(visibility);
    }
}
