package com.fsck.k9.search;

import android.os.Parcel;
import android.os.Parcelable;

public interface SearchSpecification extends Parcelable {

    /**
     * Get all the uuids of accounts this search acts on.
     * @return Array of uuids.
     */
    public String[] getAccountUuids();

    /**
     * Returns the search's name if it was named.
     * @return Name of the search.
     */
    public String getName();

    /**
     * Returns the root node of the condition tree accompanying
     * the search.
     *
     * @return Root node of conditions tree.
     */
    public ConditionsTreeNode getConditions();

    /*
     * Some meta names for certain conditions.
     */
    public static final String ALL_ACCOUNTS = "allAccounts";
    public static final String GENERIC_INBOX_NAME = "genericInboxName";

    ///////////////////////////////////////////////////////////////
    // ATTRIBUTE enum
    ///////////////////////////////////////////////////////////////
    public enum Attribute {
        CONTAINS(false), EQUALS(false), STARTSWITH(false), ENDSWITH(false),
        NOT_CONTAINS(true), NOT_EQUALS(true), NOT_STARTSWITH(true), NOT_ENDSWITH(true);

        private boolean mNegation;

        private Attribute(boolean negation) {
            this.mNegation = negation;
        }

        public String formQuery(String value) {
            String queryPart = "";

            switch (this) {
            case NOT_CONTAINS:
            case CONTAINS:
                queryPart = "'%"+value+"%'";
                break;
            case NOT_EQUALS:
            case EQUALS:
                queryPart = "'"+value+"'";
                break;
            case NOT_STARTSWITH:
            case STARTSWITH:
                queryPart = "'%"+value+"'";
                break;
            case NOT_ENDSWITH:
            case ENDSWITH:
                queryPart = "'"+value+"%'";
                break;
            default: queryPart = "'"+value+"'";
            }

            return (mNegation ? " NOT LIKE " : " LIKE ") + queryPart;
        }
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
     * 		id, html_content, internal_date, message_id,
     * 		preview, mime_type
     *
     */
    public enum Searchfield {
        SUBJECT("subject"), DATE("date"), UID("uid"), FLAG("flags"),
        SENDER("sender_list"), TO("to_list"), CC("cc_list"), FOLDER("folder_id"),
        BCC("bcc_list"), REPLY_TO("reply_to_list"), MESSAGE("text_content"),
        ATTACHMENT_COUNT("attachment_count"), DELETED("deleted"), THREAD_ROOT("thread_root"),
        ID("id");

        private String dbName;

        private Searchfield(String dbName) {
            this.dbName = dbName;
        }

        public String getDatabaseName() {
            return dbName;
        }
    }


    ///////////////////////////////////////////////////////////////
    // SearchCondition class
    ///////////////////////////////////////////////////////////////
    /**
     * This class represents 1 value for a certain search field. One
     * value consists of three things:
     * 		an attribute: equals, starts with, contains,...
     * 		a searchfield: date, flags, sender, subject,...
     * 		a value: "apple", "jesse",..
     *
     * @author dzan
     */
    public class SearchCondition implements Parcelable {
        public String value;
        public Attribute attribute;
        public Searchfield field;

        public SearchCondition(Searchfield field, Attribute attribute, String value) {
            this.value = value;
            this.attribute = attribute;
            this.field = field;
        }

        private SearchCondition(Parcel in) {
            this.value = in.readString();
            this.attribute = Attribute.values()[in.readInt()];
            this.field = Searchfield.values()[in.readInt()];
        }

        @Override
        public SearchCondition clone() {
            return new SearchCondition(field, attribute, value);
        }

        public String toHumanString() {
            return field.toString() + attribute.toString();
        }

        @Override
        public String toString() {
            return field.getDatabaseName() + attribute.formQuery(value);
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof SearchCondition) {
                SearchCondition tmp = (SearchCondition) o;
                if (tmp.attribute == attribute &&
                        tmp.field == field &&
                        tmp.value.equals(value)) {
                    return true;
                }
            }

            return false;
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