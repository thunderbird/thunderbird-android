package com.fsck.k9.activity.setup;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by lenovo on 12/25/2017.
 */

public class Intromanager {
    SharedPreferences pref;
    SharedPreferences.Editor editor;
    Context contexts;

    public Intromanager(Context contexts) {
        this.contexts = contexts;
        pref = contexts.getSharedPreferences("first",0);
        editor = pref.edit();
    }

    public void setFirst(boolean isFirst){
        editor.putBoolean("check",isFirst);
        editor.commit();
    }

    public boolean Check(){
        return pref.getBoolean("check",true);
    }
}
