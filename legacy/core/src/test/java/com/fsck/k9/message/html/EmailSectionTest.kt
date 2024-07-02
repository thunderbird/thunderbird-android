package com.fsck.k9.message.html

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isSameInstanceAs
import org.junit.Test

class EmailSectionTest {
    @Test
    fun charAt() {
        assertThat("[a]".asEmailSection()[0]).isEqualTo('a')
        assertThat(".[a]".asEmailSection()[0]).isEqualTo('a')
        assertThat("[a].".asEmailSection()[0]).isEqualTo('a')
        assertThat("[ a]".asEmailSection()[0]).isEqualTo('a')
        assertThat("[abc]".asEmailSection()[0]).isEqualTo('a')

        assertThat("[a][b]".asEmailSection()[1]).isEqualTo('b')
        assertThat("[a][bc]".asEmailSection()[1]).isEqualTo('b')
        assertThat("[ab]".asEmailSection()[1]).isEqualTo('b')
        assertThat("[ab][c]".asEmailSection()[1]).isEqualTo('b')
        assertThat("[a][b][c]".asEmailSection()[1]).isEqualTo('b')
        assertThat(".[a][b][c]".asEmailSection()[1]).isEqualTo('b')
        assertThat(".[a].[b][c]".asEmailSection()[1]).isEqualTo('b')
        assertThat(".[a].[b].[c]".asEmailSection()[1]).isEqualTo('b')
        assertThat("[ a][ b][ c]".asEmailSection()[1]).isEqualTo('b')
        assertThat("[a]..[bc]".asEmailSection()[1]).isEqualTo('b')

        assertThat("[abc]".asEmailSection()[2]).isEqualTo('c')
        assertThat("[ab][c]".asEmailSection()[2]).isEqualTo('c')
        assertThat("[a][bc]".asEmailSection()[2]).isEqualTo('c')
        assertThat("[a][b][c]".asEmailSection()[2]).isEqualTo('c')
        assertThat(".[a].[b].[c].".asEmailSection()[2]).isEqualTo('c')
        assertThat("[  a][  b][  c]".asEmailSection()[2]).isEqualTo('c')
    }

    @Test
    fun length() {
        assertThat("[]".asEmailSection().length).isEqualTo(0)
        assertThat("...[]...".asEmailSection().length).isEqualTo(0)
        assertThat("[  ]".asEmailSection().length).isEqualTo(0)
        assertThat("[ ][  ]".asEmailSection().length).isEqualTo(1)
        assertThat("[One]".asEmailSection().length).isEqualTo(3)
        assertThat("[One][Two]".asEmailSection().length).isEqualTo(6)
    }

    @Test
    fun subSequence() {
        val section = "[ One][ Two][ Three]".asEmailSection()

        assertThat(section.subSequence(0, 11)).isSameInstanceAs(section)
        assertThat(section.subSequence(0, 3).asString()).isEqualTo("One")
        assertThat(section.subSequence(0, 2).asString()).isEqualTo("On")
        assertThat(section.subSequence(1, 3).asString()).isEqualTo("ne")
        assertThat(section.subSequence(1, 2).asString()).isEqualTo("n")
        assertThat(section.subSequence(0, 4).asString()).isEqualTo("OneT")
        assertThat(section.subSequence(1, 4).asString()).isEqualTo("neT")
        assertThat(section.subSequence(1, 6).asString()).isEqualTo("neTwo")
        assertThat(section.subSequence(1, 7).asString()).isEqualTo("neTwoT")
        assertThat(section.subSequence(1, 11).asString()).isEqualTo("neTwoThree")
        assertThat(section.subSequence(3, 11).asString()).isEqualTo("TwoThree")
        assertThat(section.subSequence(4, 11).asString()).isEqualTo("woThree")
        assertThat(section.subSequence(4, 9).asString()).isEqualTo("woThr")
        assertThat(section.subSequence(6, 9).asString()).isEqualTo("Thr")
        assertThat(section.subSequence(7, 10).asString()).isEqualTo("hre")
        assertThat(section.subSequence(6, 11).asString()).isEqualTo("Three")
    }

    private fun CharSequence.asString() = StringBuilder(length).apply {
        this@asString.forEach { append(it) }
    }.toString()

    private fun String.asEmailSection(): EmailSection {
        val builder = EmailSection.Builder(this, 0)

        var startIndex = -1
        var isStartOfLine = true
        var spaces = 0
        this.forEachIndexed { index, c ->
            when (c) {
                '[' -> {
                    startIndex = index + 1
                    isStartOfLine = true
                    spaces = 0
                }

                ' ' -> if (isStartOfLine) spaces++
                ']' -> builder.addSegment(spaces, startIndex, index)
                else -> isStartOfLine = false
            }
        }

        return builder.build()
    }
}
