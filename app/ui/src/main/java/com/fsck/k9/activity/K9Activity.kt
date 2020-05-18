package com.fsck.k9.activity

import android.os.Bundle
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.fsck.k9.ui.R
import com.fsck.k9.ui.ThemeManager

abstract class K9Activity : AppCompatActivity() {
    private val base = K9ActivityCommon(this, ThemeType.DEFAULT)

    val themeManager: ThemeManager
        get() = base.themeManager

    override fun onCreate(savedInstanceState: Bundle?) {
        base.preOnCreate()
        super.onCreate(savedInstanceState)
    }

    override fun onResume() {
        base.preOnResume()
        super.onResume()
    }

    protected fun setLayout(@LayoutRes layoutResId: Int) {
        setContentView(layoutResId)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
            ?: error("K9 layouts must provide a toolbar with id='toolbar'.")

        setSupportActionBar(toolbar)
    }
}
