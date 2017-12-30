package com.fsck.k9.helper;

import android.content.Context;
import android.content.SharedPreferences;

public class IntroHelper {
    SharedPreferences pref;
    SharedPreferences.Editor editor;
    Context contexts;

    public IntroHelper(Context contexts) {
        this.contexts = contexts;
        pref = contexts.getSharedPreferences("first",0);
        editor = pref.edit();
    }

    public void setFirst(boolean isFirst){
        editor.putBoolean("check",isFirst);
        editor.commit();
    }

    public boolean check(){
        return pref.getBoolean("check",true);
    }
}
