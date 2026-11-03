package com.fsck.k9.activity.compose

import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.ScrollView
import androidx.appcompat.app.AppCompatActivity
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isSameInstanceAs
import assertk.assertions.isTrue
import com.fsck.k9.ui.R
import net.thunderbird.core.android.testing.RobolectricTest
import org.junit.Before
import org.junit.Test
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@Config(sdk = [26, 31, 36])
class ComposeKeypadNavigationTest : RobolectricTest() {
    private lateinit var activity: AppCompatActivity
    private lateinit var content: LinearLayout
    private lateinit var scrollView: ScrollView
    private lateinit var to: EditText
    private lateinit var subject: EditText
    private lateinit var body: EditText
    private lateinit var attachments: LinearLayout
    private lateinit var testSubject: ComposeKeypadNavigation
    private var enabled = true

    @Before
    fun setUp() {
        val controller = Robolectric.buildActivity(ComposeKeypadTestActivity::class.java)
        activity = controller.get()
        activity.setTheme(R.style.Theme_Legacy_Test)
        controller.setup()
        content = LinearLayout(activity).apply { orientation = LinearLayout.VERTICAL }
        to = editor(R.id.to, "recipient@example.com")
        subject = editor(R.id.subject, "Keep this subject")
        body = editor(R.id.message_content, "A long message\n".repeat(80))
        attachments = LinearLayout(activity).apply {
            id = R.id.attachments
            orientation = LinearLayout.VERTICAL
        }
        content.addView(to)
        content.addView(Button(activity).apply { text = "Expand recipients" })
        content.addView(editor(R.id.cc, "").apply { visibility = View.GONE })
        content.addView(subject)
        content.addView(body)
        content.addView(attachments)
        scrollView = ScrollView(activity).apply { addView(content) }
        activity.setContentView(scrollView)
        layout()
        testSubject = ComposeKeypadNavigation(activity) { enabled }
    }

    @Test
    fun `next and previous field should skip non-editors and hidden fields without changing text or cursor`() {
        // Arrange
        to.requestFocus()
        to.setSelection(4)
        subject.setSelection(5)
        body.setSelection(7)
        val menu = createMenu()

        // Act
        testSubject.onMenuItemSelected(menu.findItem(R.id.keypad_compose_next_field))

        // Assert
        assertThat(activity.currentFocus).isSameInstanceAs(subject)
        assertThat(subject.selectionStart).isEqualTo(5)
        assertThat(to.text.toString()).isEqualTo("recipient@example.com")

        // Act
        testSubject.onMenuItemSelected(menu.findItem(R.id.keypad_compose_next_field))

        // Assert
        assertThat(activity.currentFocus).isSameInstanceAs(body)
        assertThat(body.selectionStart).isEqualTo(7)

        // Act
        testSubject.onMenuItemSelected(menu.findItem(R.id.keypad_compose_previous_field))

        // Assert
        assertThat(activity.currentFocus).isSameInstanceAs(subject)
        assertThat(subject.selectionStart).isEqualTo(5)
        assertThat(subject.text.toString()).isEqualTo("Keep this subject")
    }

    @Test
    fun `message body action should reveal the retained cursor in a long message`() {
        // Arrange
        to.requestFocus()
        body.setSelection(body.text.length - 5)
        val cursor = body.selectionStart

        // Act
        testSubject.onMenuItemSelected(createMenu().findItem(R.id.keypad_compose_body))
        shadowOf(Looper.getMainLooper()).idle()

        // Assert
        assertThat(activity.currentFocus).isSameInstanceAs(body)
        assertThat(body.selectionStart).isEqualTo(cursor)
        assertThat(scrollView.scrollY > 0).isTrue()
        assertThat(body.text.toString()).isEqualTo("A long message\n".repeat(80))
    }

    @Test
    fun `navigation menu items should be optional and must not handle send attachment or save actions`() {
        // Arrange
        val menu = createMenu()

        // Act and assert
        testSubject.prepareMenu(menu)
        assertThat(menu.findItem(R.id.keypad_compose_next_field).isVisible).isTrue()
        for (id in listOf(R.id.send, R.id.add_attachment, R.id.save)) {
            assertThat(testSubject.onMenuItemSelected(menu.findItem(id))).isFalse()
        }

        // Act
        enabled = false
        testSubject.prepareMenu(menu)

        // Assert
        val itemIds = listOf(
            R.id.keypad_compose_next_field,
            R.id.keypad_compose_previous_field,
            R.id.keypad_compose_body,
            R.id.keypad_compose_attachments,
        )
        for (id in itemIds) {
            assertThat(menu.findItem(id).isVisible).isFalse()
            assertThat(testSubject.onMenuItemSelected(menu.findItem(id))).isFalse()
        }
    }

    @Test
    fun `removing a focused attachment should move to the adjacent attachment and then back to the body`() {
        // Arrange
        val first = attachmentRow()
        val second = attachmentRow()
        layout()
        first.findViewById<View>(R.id.attachment_delete).requestFocus()
        body.setSelection(9)

        // Act
        testSubject.removeAttachment(attachments, first, body)

        // Assert
        assertThat(activity.currentFocus).isSameInstanceAs(second.findViewById<View>(R.id.attachment_delete))

        // Act
        testSubject.removeAttachment(attachments, second, body)

        // Assert
        assertThat(activity.currentFocus).isSameInstanceAs(body)
        assertThat(body.selectionStart).isEqualTo(9)
        assertThat(attachments.childCount).isEqualTo(0)
    }

    @Test
    fun `removing an attachment without focus should leave the current editor focused`() {
        // Arrange
        val row = attachmentRow()
        layout()
        subject.requestFocus()
        subject.setSelection(3)

        // Act
        testSubject.removeAttachment(attachments, row, body)

        // Assert
        assertThat(activity.currentFocus).isSameInstanceAs(subject)
        assertThat(subject.selectionStart).isEqualTo(3)
    }

    @Test
    fun `attachments menu should become available when an attachment is added and focus its remove button`() {
        // Arrange
        val menu = createMenu()
        testSubject.prepareMenu(menu)
        val initiallyEnabled = menu.findItem(R.id.keypad_compose_attachments).isEnabled
        val row = attachmentRow()
        layout()
        to.requestFocus()

        // Act
        testSubject.prepareMenu(menu)
        testSubject.onMenuItemSelected(menu.findItem(R.id.keypad_compose_attachments))

        // Assert
        assertThat(initiallyEnabled).isFalse()
        assertThat(menu.findItem(R.id.keypad_compose_attachments).isEnabled).isTrue()
        assertThat(activity.currentFocus).isSameInstanceAs(row.findViewById<View>(R.id.attachment_delete))
    }

    private fun editor(viewId: Int, value: String) = EditText(activity).apply {
        id = viewId
        setText(value)
        isFocusableInTouchMode = true
        layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
        )
    }

    private fun attachmentRow(): LinearLayout = LinearLayout(activity).apply {
        addView(
            Button(activity).apply {
                id = R.id.attachment_delete
                isFocusableInTouchMode = true
            },
        )
        attachments.addView(this)
    }

    private fun createMenu() = PopupMenu(activity, to).menu.also {
        activity.menuInflater.inflate(R.menu.message_compose_option, it)
    }

    private fun layout() {
        activity.window.decorView.apply {
            measure(
                View.MeasureSpec.makeMeasureSpec(480, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(320, View.MeasureSpec.EXACTLY),
            )
            layout(0, 0, 480, 320)
            dispatchWindowFocusChanged(true)
        }
        shadowOf(Looper.getMainLooper()).idle()
    }
}

/** Uses the actual focus hierarchy instead of ShadowActivity's separately assigned currentFocus field. */
class ComposeKeypadTestActivity : AppCompatActivity() {
    override fun getCurrentFocus(): View? = window.decorView.findFocus()
}
