package com.fsck.k9.widget.list;


import android.content.Intent;
import android.widget.RemoteViewsService;


public class MailListViewWidgetService extends RemoteViewsService {
    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new MailListRemoteViewFactory(this.getApplicationContext());
    }
}
