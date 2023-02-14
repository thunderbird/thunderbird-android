package com.fsck.k9.mail.internet

import okio.Buffer

internal sealed class ParameterSection(
    val name: String,
    val originalName: String,
    val section: Int?,
)

internal open class ExtendedValueParameterSection(
    name: String,
    originalName: String,
    section: Int?,
    val data: Buffer,
) : ParameterSection(name, originalName, section)

internal class InitialExtendedValueParameterSection(
    name: String,
    originalName: String,
    section: Int?,
    val charsetName: String,
    val language: String?,
    data: Buffer,
) : ExtendedValueParameterSection(name, originalName, section, data)

internal class RegularValueParameterSection(
    name: String,
    originalName: String = name,
    section: Int? = null,
    val text: String,
) : ParameterSection(name, originalName, section)
