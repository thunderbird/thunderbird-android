package com.fsck.k9.mail;

import java.util.ArrayList;
import java.util.Random;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.graphics.ColorUtils;


public final class Keyword extends Flag implements Parcelable {

    private static final ArrayList<Integer> PREDEFINED_COLORS =
        new ArrayList<Integer>() {{
            add(0xFFFF0000); add(0xFFFFAA00); add(0xFFFFFF00); add(0xFFAAFF00);
            add(0xFF00FF00); add(0xFF00FFFF); add(0xFF0000FF); add(0xFFFF00FF);
        }};


    // User-defined keyword name for display as a tag.
    private String name;

    // Further user-defined keyword attributes
    private boolean visible = false;
    private int color = -1;

    private static ArrayList<Integer> nextColors = null;


    public Keyword(String code, String externalCode, String name) {
        // The caller has verified that externalCode is valid for an IMAP
        // keyowrd and not yet in use.
        super(code, externalCode);
        this.name = name;
        color = randomColor();
    }

    public static boolean matchesImapKeywordProduction(String externalCode) {
        // Rule 'flag-keyword' in RFC 3501 without comma (used as separator)
        return externalCode.matches(
            "^[\\x20-\\xff&&[^\\\\(){\\s\\x7%*\\],]]+$");
    }

    public static boolean isValidImapKeyword(String externalCode) {
        return matchesImapKeywordProduction(externalCode) &&
               !isExternalCodeOfSystemFlag(externalCode);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    // Color for text on light background.
    public int getTextColor() {
        return ColorUtils.blendARGB(color, 0xFF000000, (float) 0.2);
    }

    // Blend keyword color with background color of light theme.
    public int blendLightBackgroundColor(int lightBackgroundColor) {
        return ColorUtils.blendARGB(color, lightBackgroundColor, (float) 0.7);
    }

    // Blend keyword color with background color of dark theme.
    public int blendDarkBackgroundColor(int darkBackgroundColor) {
        return ColorUtils.blendARGB(color, darkBackgroundColor, (float) 0.3);
    }

    public int getColor() {
        return color;
    }

    private static int randomColor() {
        if (nextColors == null || nextColors.size() == 0) {
            nextColors = new ArrayList<Integer>(PREDEFINED_COLORS);
        }

        Random rnd = new Random();
        final int colorIndex = rnd.nextInt(nextColors.size());
        final int color = nextColors.get(colorIndex);
        nextColors.remove(colorIndex);
        return color;
    }

    public void setColor(int color) {
        if (color == -1) {
            this.color = randomColor();
        } else  {
            this.color = color;
        }
    }


    // Parcelable interface

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel out, int flags) {
        out.writeString(getCode());
    }

    public static final Parcelable.Creator<Keyword> CREATOR
            = new Parcelable.Creator<Keyword>() {
        public Keyword createFromParcel(Parcel in) {
            final FlagManager flagManager = FlagManager.getFlagManager();
            return flagManager.getKeywordByCode(in.readString());
        }

        public Keyword[] newArray(int size) {
            return new Keyword[size];
        }
    };

}