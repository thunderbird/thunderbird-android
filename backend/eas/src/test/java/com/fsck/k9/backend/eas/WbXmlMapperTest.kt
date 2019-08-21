package com.fsck.k9.backend.eas

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.lang.reflect.InvocationTargetException
import java.nio.charset.Charset

const val TAG_DTO = 7
const val TAG_STRING = 907
const val TAG_STRING_LIST = 908
const val TAG_INNER_STREAMABLE_ELEMENT = 909
const val TAG_INTEGER = 100
const val TAG_INNER_ELEMENT_LIST = 101
const val TAG_BOOLEAN = 905
const val TAG_BOOLEAN_FALSE = 906

class WbXmlMapperTest {

    class TestStreamedString : StreamableElement {
        lateinit var content: String

        override fun readFromStream(inputStream: InputStream) {
            content = String(inputStream.readBytes())
        }

        override fun writeToStream(outputStream: OutputStream) {
            outputStream.write(content.toByteArray())
        }
    }

    data class TestInnerElement(
            @field:Tag(TAG_INNER_STREAMABLE_ELEMENT, index = 0) val streamedString: TestStreamedString
    )

    data class TestElement(
            @field:Tag(TAG_STRING, index = 0) val string: String,
            @field:Tag(TAG_INTEGER, index = 1) val integer: Int,
            @field:Tag(TAG_INNER_ELEMENT_LIST, index = 2) val inner: List<TestInnerElement>,
            @field:Tag(TAG_STRING_LIST, index = 3) val stringList: List<String>,
            @field:Tag(TAG_BOOLEAN, index = 4) val boolean: Boolean? = null,
            @field:Tag(TAG_BOOLEAN_FALSE, index = 5) val booleanFalse: Boolean? = null
    )

    data class TestDTO(@field:Tag(TAG_DTO) val element: TestElement)

    private val testWbXmlValid = byteArrayOf(
            0x3, 0x1, 106, 0, // Header
            WbXml.SWITCH_PAGE.toByte(), // PageSwitch
            (TAG_DTO shr WbXml.PAGE_SHIFT).toByte(),
            (TAG_DTO or WbXml.CONTENT_MASK).toByte(), // Start Tag DTO
            /*  */ WbXml.SWITCH_PAGE.toByte(), // PageSwitch
            /*  */ (TAG_STRING shr WbXml.PAGE_SHIFT).toByte(),
            /*  */ ((TAG_STRING and WbXml.PAGE_MASK) or WbXml.CONTENT_MASK).toByte(), //Start Tag 'STRING'
            /*    */ WbXml.STR_I.toByte(), // Start String
            /*    */ *"str".toByteArray(),
            /*    */ 0,
            /*  */ WbXml.END.toByte(),
            /*  */ WbXml.SWITCH_PAGE.toByte(), // PageSwitch
            /*  */ (TAG_INTEGER shr WbXml.PAGE_SHIFT).toByte(),
            /*  */ ((TAG_INTEGER and WbXml.PAGE_MASK) or WbXml.CONTENT_MASK).toByte(), //Start Tag 'INTEGER'
            /*    */ WbXml.STR_I.toByte(), // Start Int
            /*    */ *"343".toByteArray(),
            /*    */ 0,
            /*  */ WbXml.END.toByte(),
            /*  */ ((TAG_INNER_ELEMENT_LIST and WbXml.PAGE_MASK) or WbXml.CONTENT_MASK).toByte(), //Start Tag 'INNER_ELEMENT_LIST'
            /*  */ WbXml.SWITCH_PAGE.toByte(), // PageSwitch
            /*    */ (TAG_INNER_STREAMABLE_ELEMENT shr WbXml.PAGE_SHIFT).toByte(),
            /*    */ ((TAG_INNER_STREAMABLE_ELEMENT and WbXml.PAGE_MASK) or WbXml.CONTENT_MASK).toByte(), //Start Tag 'INNER_STRING'
            /*      */ WbXml.STR_I.toByte(), // Start String
            /*      */ *"inner0".toByteArray(),
            /*      */ 0,
            /*    */ WbXml.END.toByte(),
            /*  */ WbXml.END.toByte(),
            /*  */ WbXml.SWITCH_PAGE.toByte(), // PageSwitch
            /*  */ (TAG_INNER_ELEMENT_LIST shr WbXml.PAGE_SHIFT).toByte(),
            /*  */ ((TAG_INNER_ELEMENT_LIST and WbXml.PAGE_MASK) or WbXml.CONTENT_MASK).toByte(), //Start Tag 'INNER_ELEMENT_LIST'
            /*  */ WbXml.SWITCH_PAGE.toByte(), // PageSwitch
            /*    */ (TAG_INNER_STREAMABLE_ELEMENT shr WbXml.PAGE_SHIFT).toByte(),
            /*    */ ((TAG_INNER_STREAMABLE_ELEMENT and WbXml.PAGE_MASK) or WbXml.CONTENT_MASK).toByte(), //Start Tag 'INNER_STRING'
            /*      */ WbXml.STR_I.toByte(), // Start String
            /*      */ *"inner1".toByteArray(),
            /*      */ 0,
            /*    */ WbXml.END.toByte(),
            /*  */ WbXml.END.toByte(),
            /*  */ ((TAG_STRING_LIST and WbXml.PAGE_MASK) or WbXml.CONTENT_MASK).toByte(), //Start Tag 'STRING_LIST'
            /*    */ WbXml.STR_I.toByte(), // Start String
            /*    */ *"abc".toByteArray(),
            /*    */ 0,
            /*  */ WbXml.END.toByte(),
            /*  */ ((TAG_STRING_LIST and WbXml.PAGE_MASK) or WbXml.CONTENT_MASK).toByte(), //Start Tag 'STRING_LIST'
            /*    */ WbXml.STR_I.toByte(), // Start String
            /*    */ *"123".toByteArray(),
            /*    */ 0,
            /*  */ WbXml.END.toByte(),
            /*  */ (TAG_BOOLEAN and WbXml.PAGE_MASK).toByte(),
            WbXml.END.toByte()
    )

    @Test
    fun serialize_shouldCreateXmlWb() {
        val element = TestDTO(
                TestElement(
                        "str",
                        343,
                        listOf(
                                TestInnerElement(
                                        TestStreamedString().apply { content = "inner0" }
                                ),
                                TestInnerElement(
                                        TestStreamedString().apply { content = "inner1" }
                                )
                        ),
                        listOf(
                                "abc",
                                "123"
                        ),
                        true,
                        false
                )
        )

        val result = ByteArrayOutputStream().apply {
            WbXmlMapper.serialize(element, this)
        }.toByteArray()

        assertArrayEquals(result, testWbXmlValid)
    }

    @Test
    fun parse_shouldCreateDTO() {
        val result = WbXmlMapper.parse<TestDTO>(testWbXmlValid.inputStream())

        assertEquals(result.element.string, "str")
        assertEquals(result.element.integer, 343)
        assertEquals(result.element.inner[0].streamedString.content, "inner0")
        assertEquals(result.element.inner[1].streamedString.content, "inner1")
        assertEquals(result.element.stringList, listOf("abc", "123"))
    }

    data class SimpleTestElement(
            @field:Tag(TAG_STRING, index = 0) val string: String
    )

    data class SimpleTestDTO(@field:Tag(TAG_DTO) val element: SimpleTestElement? = null)

    @Test
    fun parse_skip_ignored_elements_shouldCreateDTO() {
        val result = WbXmlMapper.parse<SimpleTestDTO>(testWbXmlValid.inputStream())

        assertEquals(result, SimpleTestDTO(
                SimpleTestElement(
                        "str"
                )
        ))
    }

    @Test(expected = IOException::class)
    fun parse_invalid_stri_shouldThrow() {
        WbXmlMapper.parse<SimpleTestDTO>(byteArrayOf(
                0x3, 0x1, 106, 0, // Header
                WbXml.STR_I.toByte(),
                *"abc".toByteArray(),
                0
        ).inputStream())
    }

    @Test(expected = InvocationTargetException::class)
    fun parse_element_missing_shouldThrow() {
        WbXmlMapper.parse<TestDTO>(byteArrayOf(
                0x3, 0x1, 106, 0 // Header
        ).inputStream())
    }
}
