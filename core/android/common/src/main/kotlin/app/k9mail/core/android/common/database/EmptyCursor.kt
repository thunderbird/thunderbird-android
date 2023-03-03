package app.k9mail.core.android.common.database

import android.database.AbstractCursor

/**
 * A dummy class that provides an empty cursor
 */
class EmptyCursor : AbstractCursor() {
    override fun getCount() = 0

    override fun getColumnNames() = arrayOf<String>()

    override fun getString(column: Int) = null

    override fun getShort(column: Int): Short = 0

    override fun getInt(column: Int) = 0

    override fun getLong(column: Int): Long = 0

    override fun getFloat(column: Int) = 0f

    override fun getDouble(column: Int) = 0.0

    override fun isNull(column: Int) = true
}
