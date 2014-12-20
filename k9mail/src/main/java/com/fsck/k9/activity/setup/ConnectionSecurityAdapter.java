package com.fsck.k9.activity.setup;

import android.content.Context;
import android.widget.ArrayAdapter;

import com.fsck.k9.mail.ConnectionSecurity;


class ConnectionSecurityAdapter extends ArrayAdapter<ConnectionSecurityHolder> {
    public ConnectionSecurityAdapter(Context context, int resource, ConnectionSecurityHolder[] securityTypes) {
        super(context, resource, securityTypes);
    }

    public static ConnectionSecurityAdapter get(Context context) {
        return get(context, ConnectionSecurity.values());
    }

    public static ConnectionSecurityAdapter get(Context context,
                                                ConnectionSecurity[] items) {
        ConnectionSecurityHolder[] holders = new ConnectionSecurityHolder[items.length];
        for (int i = 0; i < items.length; i++) {
            holders[i] = new ConnectionSecurityHolder(items[i], context.getResources());
        }
        ConnectionSecurityAdapter securityTypesAdapter = new ConnectionSecurityAdapter(context,
                android.R.layout.simple_spinner_item, holders);
        securityTypesAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        return securityTypesAdapter;
    }

    public int getConnectionSecurityPosition(ConnectionSecurity connectionSecurity) {
        for (int i=0; i<getCount(); i++) {
            if (getItem(i).connectionSecurity == connectionSecurity) {
                return i;
            }
        }
        return -1;
    }
}
