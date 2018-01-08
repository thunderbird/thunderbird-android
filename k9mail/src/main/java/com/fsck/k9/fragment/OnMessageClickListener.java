package com.fsck.k9.fragment;


import android.database.Cursor;


public interface OnMessageClickListener {
    void onMessageClick(Cursor cursor, int position);
}