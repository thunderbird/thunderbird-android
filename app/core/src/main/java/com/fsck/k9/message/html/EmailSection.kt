package com.fsck.k9.message.html

/**
 * Represents a section of an email's plain text body.
 *
 * See [EmailSectionExtractor].
 */
class EmailSection private constructor(builder: Builder) : CharSequence {
    val quoteDepth = builder.quoteDepth
    private val text = builder.text
    private val segments: List<Segment> = if (builder.indent == 0) {
        builder.segments.toList()
    } else {
        builder.segments.map { segment ->
            val minLength = if (text[segment.endIndex - 1] == '\n') 1 else 0
            val adjustedStartIndex = (segment.startIndex + builder.indent).coerceAtMost(segment.endIndex - minLength)
            Segment(adjustedStartIndex, segment.endIndex)
        }
    }

    override val length = segments.map { it.endIndex - it.startIndex }.sum()

    override fun get(index: Int): Char {
        require(index in 0..(length - 1)) { "index: $index; length: $length" }

        var offset = index
        for (i in 0..(segments.size - 1)) {
            val segment = segments[i]
            val segmentLength = segment.endIndex - segment.startIndex
            if (offset < segmentLength) {
                return text[segment.startIndex + offset]
            }
            offset -= segmentLength
        }

        throw AssertionError()
    }

    override fun subSequence(startIndex: Int, endIndex: Int): CharSequence {
        require(startIndex in 0..(length - 1)) { "startIndex: $startIndex; length: $length" }
        require(endIndex in 0..length) { "endIndex: $endIndex; length: $length" }
        require(startIndex <= endIndex) { "startIndex > endIndex" }

        if (startIndex == endIndex) return ""
        if (startIndex == 0 && endIndex == length) return this

        val builder = Builder(text, quoteDepth)

        val (startSegmentIndex, startOffset) = findSegmentIndexAndOffset(startIndex)
        val (endSegmentIndex, endOffset) = findSegmentIndexAndOffset(endIndex, isEndIndex = true)
        val startSegment = segments[startSegmentIndex]

        if (startSegmentIndex == endSegmentIndex) {
            builder.addSegment(0, startSegment.startIndex + startOffset, startSegment.startIndex + endOffset)
            return builder.build()
        }

        if (startOffset == 0) {
            builder.addSegment(startSegment)
        } else {
            builder.addSegment(0, startSegment.startIndex + startOffset, startSegment.endIndex)
        }

        for (segmentIndex in startSegmentIndex + 1 until endSegmentIndex) {
            builder.addSegment(segments[segmentIndex])
        }

        val endSegment = segments[endSegmentIndex]
        if (endSegment.startIndex + endOffset == endSegment.endIndex) {
            builder.addSegment(endSegment)
        } else {
            builder.addSegment(0, endSegment.startIndex, endSegment.startIndex + endOffset)
        }

        return builder.build()
    }

    private fun findSegmentIndexAndOffset(index: Int, isEndIndex: Boolean = false): Pair<Int, Int> {
        var offset = index
        segments.forEachIndexed { segmentIndex, segment ->
            val segmentLength = segment.endIndex - segment.startIndex
            if (offset < segmentLength || (isEndIndex && offset == segmentLength)) {
                return Pair(segmentIndex, offset)
            }
            offset -= segmentLength
        }

        throw AssertionError()
    }

    override fun toString() = StringBuilder().apply {
        segments.forEach {
            append(text, it.startIndex, it.endIndex)
        }
    }.toString()

    internal data class Segment(val startIndex: Int, val endIndex: Int)

    class Builder(val text: String, val quoteDepth: Int) {
        internal val segments: MutableList<Segment> = mutableListOf()
        internal var indent = Int.MAX_VALUE

        val hasSegments
            get() = segments.isNotEmpty()

        fun addSegment(leadingSpaces: Int, startIndex: Int, endIndex: Int): Builder {
            indent = minOf(indent, leadingSpaces)
            segments.add(Segment(startIndex, endIndex))
            return this
        }

        fun addBlankSegment(startIndex: Int, endIndex: Int) {
            segments.add(Segment(startIndex, endIndex))
        }

        internal fun addSegment(segment: Segment) {
            indent = 0
            segments.add(segment)
        }

        fun build(): EmailSection {
            if (indent == Int.MAX_VALUE) indent = 0
            return EmailSection(this)
        }
    }
}
