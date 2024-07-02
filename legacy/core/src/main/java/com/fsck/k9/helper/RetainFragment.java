package com.fsck.k9.helper;


import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;


public class RetainFragment<T> extends Fragment {
    private T data;
    private boolean cleared;

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

    public static <T> RetainFragment<T> findOrNull(FragmentManager fm, String tag) {
        // noinspection unchecked, we know this is the the right type
        return (RetainFragment<T>) fm.findFragmentByTag(tag);
    }

    public static <T> RetainFragment<T> findOrCreate(FragmentManager fm, String tag) {
        // noinspection unchecked, we know this is the the right type
        RetainFragment<T> retainFragment = (RetainFragment<T>) fm.findFragmentByTag(tag);

        if (retainFragment == null || retainFragment.cleared) {
            retainFragment = new RetainFragment<>();
            fm.beginTransaction()
                    .add(retainFragment, tag)
                    .commitAllowingStateLoss();
        }

        return retainFragment;
    }

    public void clearAndRemove(FragmentManager fm) {
        data = null;
        cleared = true;

        if (fm.isDestroyed()) {
            return;
        }

        fm.beginTransaction()
                .remove(this)
                .commitAllowingStateLoss();
    }
}
