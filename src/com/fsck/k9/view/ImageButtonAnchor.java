package com.fsck.k9.view;

import com.actionbarsherlock.internal.view.View_HasStateListenerSupport;
import com.actionbarsherlock.internal.view.View_OnAttachStateChangeListener;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ImageButton;


public class ImageButtonAnchor extends ImageButton implements View_HasStateListenerSupport {

    public ImageButtonAnchor(Context context) {
        super(context);
    }

    public ImageButtonAnchor(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ImageButtonAnchor(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public void addOnAttachStateChangeListener(View_OnAttachStateChangeListener listener) {
        // TODO Auto-generated method stub

    }

    @Override
    public void removeOnAttachStateChangeListener(View_OnAttachStateChangeListener listener) {
        // TODO Auto-generated method stub

    }
}
