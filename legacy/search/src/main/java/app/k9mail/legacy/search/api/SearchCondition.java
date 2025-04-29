package app.k9mail.legacy.search.api;


import android.os.Parcel;
import android.os.Parcelable;


/**
 * This class represents 1 value for a certain search field. One value consists of three things: an attribute: equals,
 * starts with, contains,... a searchfield: date, flags, sender, subject,... a value: "apple", "jesse",..
 *
 * @author dzan
 */
public class SearchCondition implements Parcelable {
    public final String value;
    public final SearchAttribute attribute;
    public final SearchField field;

    public SearchCondition(SearchField field, SearchAttribute attribute, String value) {
        this.value = value;
        this.attribute = attribute;
        this.field = field;
    }

    private SearchCondition(Parcel in) {
        this.value = in.readString();
        this.attribute = SearchAttribute.values()[in.readInt()];
        this.field = SearchField.values()[in.readInt()];
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

    public static final Creator<SearchCondition> CREATOR =
        new Creator<SearchCondition>() {

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
