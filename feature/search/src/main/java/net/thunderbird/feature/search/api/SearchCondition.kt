package net.thunderbird.feature.search.api

import android.os.Parcel
import android.os.Parcelable

/**
 * This class represents 1 value for a certain search field. One value consists of three things: an attribute: equals,
 * starts with, contains,... a searchfield: date, flags, sender, subject,... a value: "apple", "jesse",..
 *
 * @author dzan
 */
class SearchCondition : Parcelable {
    @JvmField
    val value: String?

    @JvmField
    val attribute: SearchAttribute

    @JvmField
    val field: SearchField

    constructor(field: SearchField, attribute: SearchAttribute, value: String?) {
        this.value = value
        this.attribute = attribute
        this.field = field
    }

    private constructor(parcel: Parcel) {
        this.value = parcel.readString()
        this.attribute = SearchAttribute.values()[parcel.readInt()]
        this.field = SearchField.values()[parcel.readInt()]
    }

    override fun equals(other: Any?): Boolean {
        if (other is SearchCondition) {
            val tmp = other
            return tmp.attribute == attribute &&
                tmp.field == field &&
                tmp.value == value
        }

        return false
    }

    override fun hashCode(): Int {
        var result = 1
        result = 31 * result + attribute.hashCode()
        result = 31 * result + field.hashCode()
        result = 31 * result + value.hashCode()

        return result
    }

    override fun describeContents(): Int = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(value)
        dest.writeInt(attribute.ordinal)
        dest.writeInt(field.ordinal)
    }

    companion object CREATOR : Parcelable.Creator<SearchCondition> {
        override fun createFromParcel(parcel: Parcel): SearchCondition {
            return SearchCondition(parcel)
        }

        override fun newArray(size: Int): Array<SearchCondition?> {
            return arrayOfNulls(size)
        }
    }
}
