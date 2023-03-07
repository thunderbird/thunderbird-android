package app.k9mail.core.android.common.contact

import android.Manifest
import android.content.ContentResolver
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.provider.ContactsContract
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNull
import assertk.assertions.isTrue
import kotlin.test.Test
import org.junit.Before
import org.junit.runner.RunWith
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows

@RunWith(RobolectricTestRunner::class)
internal class ContentResolverContactDataSourceTest {

    private val contentResolver = mock<ContentResolver>()

    private val testSubject = ContentResolverContactDataSource(
        context = RuntimeEnvironment.getApplication(),
        contentResolver = contentResolver,
    )

    @Before
    fun setUp() {
        Shadows.shadowOf(RuntimeEnvironment.getApplication()).grantPermissions(Manifest.permission.READ_CONTACTS)
    }

    @Test
    fun `getContactForEmail() returns null if permission is not granted`() {
        Shadows.shadowOf(RuntimeEnvironment.getApplication()).denyPermissions(Manifest.permission.READ_CONTACTS)

        val result = testSubject.getContactFor(CONTACT_EMAIL_ADDRESS)

        assertThat(result).isNull()
    }

    @Test
    fun `getContactForEmail() returns null if no contact is found`() {
        setupContactProvider(setupEmptyContactCursor())

        val result = testSubject.getContactFor(CONTACT_EMAIL_ADDRESS)

        assertThat(result).isNull()
    }

    @Test
    fun `getContactForEmail() returns contact if a contact is found`() {
        setupContactProvider(setupContactCursor())

        val result = testSubject.getContactFor(CONTACT_EMAIL_ADDRESS)

        assertThat(result).isEqualTo(CONTACT)
    }

    @Test
    fun `hasContactForEmail() returns false if permission is not granted`() {
        Shadows.shadowOf(RuntimeEnvironment.getApplication()).denyPermissions(Manifest.permission.READ_CONTACTS)

        val result = testSubject.hasContactFor(CONTACT_EMAIL_ADDRESS)

        assertThat(result).isFalse()
    }

    @Test
    fun `hasContactForEmail() returns false if no contact is found`() {
        setupContactProvider(setupEmptyContactCursor())

        val result = testSubject.hasContactFor(CONTACT_EMAIL_ADDRESS)

        assertThat(result).isFalse()
    }

    @Test
    fun `hasContactForEmail() returns true if a contact is found`() {
        setupContactProvider(setupContactCursor())

        val result = testSubject.hasContactFor(CONTACT_EMAIL_ADDRESS)

        assertThat(result).isTrue()
    }

    private fun setupContactProvider(contactCursor: Cursor) {
        val emailUri = Uri.withAppendedPath(
            ContactsContract.CommonDataKinds.Email.CONTENT_LOOKUP_URI,
            Uri.encode(CONTACT_EMAIL_ADDRESS.address),
        )

        contentResolver.stub {
            on {
                query(eq(emailUri), eq(PROJECTION), anyOrNull(), anyOrNull(), eq(SORT_ORDER))
            } doReturn contactCursor
        }
    }

    private fun setupEmptyContactCursor(): Cursor {
        return MatrixCursor(PROJECTION)
    }

    private fun setupContactCursor(): Cursor {
        return MatrixCursor(PROJECTION).apply {
            addRow(arrayOf(CONTACT_ID, CONTACT_NAME, CONTACT_PHOTO_URI, CONTACT_LOOKUP_KEY))
        }
    }

    private companion object {
        val PROJECTION = arrayOf(
            ContactsContract.CommonDataKinds.Email._ID,
            ContactsContract.CommonDataKinds.Identity.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Photo.PHOTO_URI,
            ContactsContract.Contacts.LOOKUP_KEY,
        )

        const val SORT_ORDER = ContactsContract.Contacts.DISPLAY_NAME +
            ", " + ContactsContract.CommonDataKinds.Email._ID
    }
}
