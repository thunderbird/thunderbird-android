package com.fsck.k9.activity.setup.outgoing;

import android.os.Parcel;
import android.os.Parcelable;

import com.fsck.k9.mail.AuthType;
import com.fsck.k9.mail.ConnectionSecurity;

class OutgoingState implements Parcelable {
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

    OutgoingState(AuthType authType, ConnectionSecurity connectionSecurity) {
        this.authType = authType;
        this.connectionSecurity = connectionSecurity;
    }

    protected OutgoingState(Parcel in) {
        int tmpAuthType = in.readInt();
        this.authType = tmpAuthType == -1 ? null : AuthType.values()[tmpAuthType];
        int tmpConnectionSecurity = in.readInt();
        this.connectionSecurity = tmpConnectionSecurity == -1 ? null : ConnectionSecurity.values()[tmpConnectionSecurity];
    }

    public static final Creator<OutgoingState> CREATOR = new Creator<OutgoingState>() {
        @Override
        public OutgoingState createFromParcel(Parcel source) {
            return new OutgoingState(source);
        }

        @Override
        public OutgoingState[] newArray(int size) {
            return new OutgoingState[size];
        }
    };
}
