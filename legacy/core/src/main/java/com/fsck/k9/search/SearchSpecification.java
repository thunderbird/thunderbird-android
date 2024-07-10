package com.fsck.k9.search;

import android.os.Parcel;
import android.os.Parcelable;

public interface SearchSpecification extends Parcelable {

    /**
     * Get all the uuids of accounts this search acts on.
     * @return Array of uuids.
     */
    String[] getAccountUuids();

    /**
     * Returns the root node of the condition tree accompanying
     * the search.
     *
     * @return Root node of conditions tree.
     */
    ConditionsTreeNode getConditions();

    ///////////////////////////////////////////////////////////////
    // ATTRIBUTE enum
    ///////////////////////////////////////////////////////////////
    enum Attribute {
        CONTAINS,
        NOT_CONTAINS,

        EQUALS,
        NOT_EQUALS,

        STARTSWITH,
        NOT_STARTSWITH,

        ENDSWITH,
        NOT_ENDSWITH
    }

    ///////////////////////////////////////////////////////////////
    // SEARCHFIELD enum
    ///////////////////////////////////////////////////////////////
    /*
     * Using an enum in order to have more robust code. Users ( & coders )
     * are prevented from passing illegal fields. No database overhead
     * when invalid fields passed.
     *
     * By result, only the fields in here are searchable.
     *
     * Fields not in here at this moment ( and by effect not searchable ):
     *      id, html_content, internal_date, message_id,
     *      preview, mime_type
     *
     */
    enum SearchField {
        SUBJECT,
        DATE,
        UID,
        FLAG,
        SENDER,
        TO,
        CC,
        FOLDER,
        BCC,
        REPLY_TO,
        MESSAGE_CONTENTS,
        ATTACHMENT_COUNT,
        DELETED,
        THREAD_ID,
        ID,
        INTEGRATE,
        NEW_MESSAGE,
        READ,
        FLAGGED,
        DISPLAY_CLASS
    }


    ///////////////////////////////////////////////////////////////
    // SearchCondition class
    ///////////////////////////////////////////////////////////////
    /**
     * This class represents 1 value for a certain search field. One
     * value consists of three things:
     *      an attribute: equals, starts with, contains,...
     *      a searchfield: date, flags, sender, subject,...
     *      a value: "apple", "jesse",..
     *
     * @author dzan
     */
    class SearchCondition implements Parcelable {
        public final String value;
        public final Attribute attribute;
        public final SearchField field;

        public SearchCondition(SearchField field, Attribute attribute, String value) {
            this.value = value;
            this.attribute = attribute;
            this.field = field;
        }

        private SearchCondition(Parcel in) {
            this.value = in.readString();
            this.attribute = Attribute.values()[in.readInt()];
            this.field = SearchField.values()[in.readInt()];
        }

        @Override
        public SearchCondition clone() {
            return new SearchCondition(field, attribute, value);
        }

        public String toHumanString() {
            return field.toString() + attribute.toString();
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof SearchCondition) {
                SearchCondition tmp = (SearchCondition) o;
                return tmp.attribute == attribute &&
                        tmp.field == field &&
                        tmp.value.equals(value);
            }

            return false;
        }

        @Override
        public int hashCode() {
            int result = 1;
            result = 31 * result + attribute.hashCode();
            result = 31 * result + field.hashCode();
            result = 31 * result + value.hashCode();

            return result;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(value);
            dest.writeInt(attribute.ordinal());
            dest.writeInt(field.ordinal());
        }

        public static final Parcelable.Creator<SearchCondition> CREATOR =
                new Parcelable.Creator<SearchCondition>() {

            @Override
            public SearchCondition createFromParcel(Parcel in) {
                return new SearchCondition(in);
            }

            @Override
            public SearchCondition[] newArray(int size) {
                return new SearchCondition[size];
            }
        };
    }
}
