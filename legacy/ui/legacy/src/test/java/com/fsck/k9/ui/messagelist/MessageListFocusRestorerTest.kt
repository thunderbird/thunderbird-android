package com.fsck.k9.ui.messagelist

import android.app.Activity
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import app.k9mail.legacy.message.controller.MessageReference
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNull
import assertk.assertions.isSameInstanceAs
import assertk.assertions.isTrue
import net.thunderbird.core.android.testing.RobolectricTest
import net.thunderbird.feature.account.AccountIdFactory
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@Config(sdk = [26, 31, 36])
class MessageListFocusRestorerTest : RobolectricTest() {
    private val activityController = Robolectric.buildActivity(Activity::class.java).setup()
    private val activity = activityController.get()
    private val accountId = AccountIdFactory.of("6b84207b-25de-4dab-97c3-953bbf03fec6")
    private val messages = (1..40).map { Row(it.toLong(), MessageReference(accountId, 1, "$it")) }
    private val adapter = RowAdapter(messages)
    private val list = RecyclerView(activity).apply {
        layoutManager = LinearLayoutManager(activity)
        itemAnimator = null
        adapter = this@MessageListFocusRestorerTest.adapter
    }
    private lateinit var testSubject: MessageListFocusRestorer

    @Before
    fun setUp() {
        activity.setContentView(list)
        activityController.windowFocusChanged(true)
        val decor = activity.window.decorView
        decor.measure(
            View.MeasureSpec.makeMeasureSpec(480, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(800, View.MeasureSpec.EXACTLY),
        )
        decor.layout(0, 0, 480, 800)
        settleList()
    }

    @After
    fun tearDown() {
        if (this::testSubject.isInitialized) testSubject.close()
        activityController.pause().stop().destroy()
    }

    @Test
    fun `offscreen return target is revealed by stable identity after the list changes order`() {
        // Arrange
        val target = messages[2]
        var ready = false
        createTestSubject { ready }
        testSubject.revealAndFocus(target.reference)
        adapter.rows = messages.filterNot { it == target } + target

        // Act
        ready = true
        testSubject.retry()
        settleList()

        // Assert
        assertThat(list.focusedChild?.let(list::findContainingViewHolder)?.itemId).isEqualTo(target.id)
        assertThat(testSubject.isPending).isFalse()
    }

    @Test
    fun `removed return target leaves container focus and a late reload does not steal focus`() {
        // Arrange
        val removed = messages[1]
        adapter.rows = listOf(messages[0])
        settleList()
        createTestSubject { true }

        // Act
        testSubject.revealAndFocus(removed.reference)
        settleList()

        // Assert
        assertThat(list.findFocus()).isSameInstanceAs(list)
        assertThat(list.focusedChild).isNull()
        assertThat(testSubject.isPending).isFalse()
        adapter.rows = listOf(messages[0], removed)
        settleList()
        assertThat(list.findFocus()).isSameInstanceAs(list)
    }

    @Test
    fun `removing the restored focused message does not focus its replacement`() {
        // Arrange
        createTestSubject { true }
        testSubject.revealAndFocus(messages[1].reference)
        settleList()
        assertThat(list.focusedChild?.let(list::findContainingViewHolder)?.itemId).isEqualTo(messages[1].id)

        // Act
        adapter.rows = messages.filterNot { it == messages[1] }
        settleList()

        // Assert
        assertThat(list.findFocus()).isSameInstanceAs(list)
        assertThat(list.focusedChild).isNull()
    }

    @Test
    fun `cancelled restoration never takes focus back after another row is chosen`() {
        // Arrange
        var ready = false
        createTestSubject { ready }
        testSubject.revealAndFocus(messages[2].reference)
        settleList()
        testSubject.cancelPending()
        val chosen = list.findViewHolderForAdapterPosition(0)!!.itemView
        assertThat(chosen.requestFocusFromTouch()).isTrue()

        // Act
        ready = true
        adapter.notifyItemRangeChanged(0, adapter.itemCount)
        testSubject.retry()
        settleList()

        // Assert
        assertThat(list.findFocus()).isSameInstanceAs(chosen)
        assertThat(testSubject.isPending).isFalse()
    }

    private fun createTestSubject(canRestoreFocus: () -> Boolean) {
        testSubject = MessageListFocusRestorer(
            recyclerView = list,
            findPosition = { reference -> adapter.rows.indexOfFirst { it.reference == reference }.takeIf { it >= 0 } },
            canRestoreFocus = canRestoreFocus,
        )
    }

    private fun settleList() {
        repeat(3) {
            shadowOf(Looper.getMainLooper()).idle()
            list.measure(
                View.MeasureSpec.makeMeasureSpec(480, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(600, View.MeasureSpec.EXACTLY),
            )
            list.layout(0, 0, 480, 600)
        }
        shadowOf(Looper.getMainLooper()).idle()
    }

    private data class Row(val id: Long, val reference: MessageReference)

    private class RowAdapter(initialRows: List<Row>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        var rows = initialRows
            set(value) {
                field = value
                notifyDataSetChanged()
            }

        init {
            setHasStableIds(true)
        }

        override fun getItemCount(): Int = rows.size
        override fun getItemId(position: Int): Long = rows[position].id
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
            object : RecyclerView.ViewHolder(
                Button(parent.context).apply {
                    layoutParams = RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 120)
                },
            ) {}

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            (holder.itemView as Button).text = "Fixture row ${rows[position].id}"
        }
    }
}
