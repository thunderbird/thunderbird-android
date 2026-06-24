package com.fsck.k9.textblocks;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Objects;
import java.util.UUID;

/**
 * Datenmodell für einen Textbaustein
 */
public class TextBlock implements Parcelable {
    private final String id;
    private String name;
    private String content;

    public TextBlock(String name, String content) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.content = content;
    }

    public TextBlock(String id, String name, String content) {
        this.id = id;
        this.name = name;
        this.content = content;
    }

    protected TextBlock(Parcel in) {
        id = in.readString();
        name = in.readString();
        content = in.readString();
    }

    public static final Creator<TextBlock> CREATOR = new Creator<TextBlock>() {
        @Override
        public TextBlock createFromParcel(Parcel in) {
            return new TextBlock(in);
        }

        @Override
        public TextBlock[] newArray(int size) {
            return new TextBlock[size];
        }
    };

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TextBlock textBlock = (TextBlock) o;
        return Objects.equals(id, textBlock.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeString(name);
        dest.writeString(content);
    }
}