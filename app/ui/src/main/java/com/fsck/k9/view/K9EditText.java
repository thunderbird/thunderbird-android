package com.fsck.k9.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.v7.widget.AppCompatEditText;
import android.util.AttributeSet;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;


import com.fsck.k9.ui.R;


public class K9EditText extends AppCompatEditText {

    private final int IME_FLAG_NO_PERSONALIZED_LEARNING = 0x1000000;


    private boolean mIncognitoEnabled = false;

    public K9EditText(Context context) {
        super(context);
    }

    public K9EditText(Context context, AttributeSet attrs) {
        super(context, attrs);

        if(attrs != null){
            TypedArray a = getContext().obtainStyledAttributes(attrs,
                    R.styleable.K9EditText, 0, 0);
            mIncognitoEnabled = a.getBoolean(R.styleable.K9EditText_is_incognito,mIncognitoEnabled);
            a.recycle();
        }
    }

    public K9EditText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
        if(mIncognitoEnabled) addIncognitoFlag(outAttrs);
        return super.onCreateInputConnection(outAttrs);
    }


    private void addIncognitoFlag(EditorInfo outAttrs){
        outAttrs.imeOptions = outAttrs.imeOptions | IME_FLAG_NO_PERSONALIZED_LEARNING;
    }


}
