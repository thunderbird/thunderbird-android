package com.fsck.k9.activity.setup;

import android.os.Parcel;

import com.fsck.k9.BaseState;
import com.fsck.k9.mail.AuthType;
import com.fsck.k9.mail.ConnectionSecurity;


public class IncomingAndOutgoingState implements BaseState {

    AuthType authType;
    ConnectionSecurity connectionSecurity;

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.authType == null ? -1 : this.authType.ordinal());
        dest.writeInt(this.connectionSecurity == null ? -1 : this.connectionSecurity.ordinal());
    }

    public IncomingAndOutgoingState(AuthType authType, ConnectionSecurity connectionSecurity) {
        this.authType = authType;
        this.connectionSecurity = connectionSecurity;
    }

    protected IncomingAndOutgoingState(Parcel in) {
        int tmpAuthType = in.readInt();
        this.authType = tmpAuthType == -1 ? null : AuthType.values()[tmpAuthType];
        int tmpConnectionSecurity = in.readInt();
        this.connectionSecurity = tmpConnectionSecurity == -1 ? null : ConnectionSecurity.values()[tmpConnectionSecurity];
    }

    public static final Creator<IncomingAndOutgoingState> CREATOR = new Creator<IncomingAndOutgoingState>() {
        @Override
        public IncomingAndOutgoingState createFromParcel(Parcel source) {
            return new IncomingAndOutgoingState(source);
        }

        @Override
        public IncomingAndOutgoingState[] newArray(int size) {
            return new IncomingAndOutgoingState[size];
        }
    };

    public AuthType getAuthType() {
        return authType;
    }

    public ConnectionSecurity getConnectionSecurity() {
        return connectionSecurity;
    }
}
