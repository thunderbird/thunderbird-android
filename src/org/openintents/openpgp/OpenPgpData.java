/*
 * Copyright (C) 2013 Dominik Schürmann <dominik@dominikschuermann.de>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.openintents.openpgp;

import android.net.Uri;
import android.os.Parcel;
import android.os.ParcelFileDescriptor;
import android.os.Parcelable;

public class OpenPgpData implements Parcelable {
    public static final int TYPE_STRING = 0;
    public static final int TYPE_BYTE_ARRAY = 1;
    public static final int TYPE_FILE_DESCRIPTOR = 2;
    public static final int TYPE_URI = 3;

    int type;

    String string;
    byte[] bytes = new byte[0];
    ParcelFileDescriptor fileDescriptor;
    Uri uri;

    public int getType() {
        return type;
    }

    public String getString() {
        return string;
    }

    public byte[] getBytes() {
        return bytes;
    }

    public ParcelFileDescriptor getFileDescriptor() {
        return fileDescriptor;
    }

    public Uri getUri() {
        return uri;
    }

    public OpenPgpData() {

    }

    /**
     * Not a real constructor. This can be used to define requested output type.
     * 
     * @param type
     */
    public OpenPgpData(int type) {
        this.type = type;
    }

    public OpenPgpData(String string) {
        this.string = string;
        this.type = TYPE_STRING;
    }

    public OpenPgpData(byte[] bytes) {
        this.bytes = bytes;
        this.type = TYPE_BYTE_ARRAY;
    }

    public OpenPgpData(ParcelFileDescriptor fileDescriptor) {
        this.fileDescriptor = fileDescriptor;
        this.type = TYPE_FILE_DESCRIPTOR;
    }

    public OpenPgpData(Uri uri) {
        this.uri = uri;
        this.type = TYPE_URI;
    }

    public OpenPgpData(OpenPgpData b) {
        this.string = b.string;
        this.bytes = b.bytes;
        this.fileDescriptor = b.fileDescriptor;
        this.uri = b.uri;
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(string);
        dest.writeInt(bytes.length);
        dest.writeByteArray(bytes);
        dest.writeParcelable(fileDescriptor, 0);
        dest.writeParcelable(uri, 0);
    }

    public static final Creator<OpenPgpData> CREATOR = new Creator<OpenPgpData>() {
        public OpenPgpData createFromParcel(final Parcel source) {
            OpenPgpData vr = new OpenPgpData();
            vr.string = source.readString();
            vr.bytes = new byte[source.readInt()];
            source.readByteArray(vr.bytes);
            vr.fileDescriptor = source.readParcelable(ParcelFileDescriptor.class.getClassLoader());
            vr.fileDescriptor = source.readParcelable(Uri.class.getClassLoader());
            return vr;
        }

        public OpenPgpData[] newArray(final int size) {
            return new OpenPgpData[size];
        }
    };

}
