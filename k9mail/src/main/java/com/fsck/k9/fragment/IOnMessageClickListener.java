package com.fsck.k9.fragment;


import android.database.Cursor;


public interface IOnMessageClickListener {
    void onMessageClick(Cursor cursor, int position);
}
