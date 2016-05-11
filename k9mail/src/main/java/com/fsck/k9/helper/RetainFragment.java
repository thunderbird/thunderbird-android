package com.fsck.k9.helper;


import android.app.Fragment;
import android.app.FragmentManager;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;


public class RetainFragment<T> extends Fragment {
    private T data;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setRetainInstance(true);
    }

    public T getData() {
        return data;
    }

    public boolean hasData() {
        return data != null;
    }

    public void setData(T data) {
        this.data = data;
    }

    public static <T> RetainFragment<T> findOrCreate(FragmentManager fm, String tag) {
        // noinspection unchecked, we know this is the the right type
        RetainFragment<T> retainFragment = (RetainFragment<T>) fm.findFragmentByTag(tag);

        if (retainFragment == null) {
            retainFragment = new RetainFragment<>();
            fm.beginTransaction()
                    .add(retainFragment, tag)
                    .commitAllowingStateLoss();
        }

        return retainFragment;
    }

    public void clearAndRemove(FragmentManager fm) {
        data = null;

        if (VERSION.SDK_INT >= VERSION_CODES.JELLY_BEAN_MR1 && fm.isDestroyed()) {
            return;
        }

        fm.beginTransaction()
                .remove(this)
                .commitAllowingStateLoss();
    }
}