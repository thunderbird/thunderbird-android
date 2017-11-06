package com.fsck.k9.fragment;
import android.database.Cursor;
/**
 Created by Kamil Rajtar on 05.11.17. */

public interface IOnMessageClickListener{
	void onMessageClick(Cursor cursor,int position);
}
