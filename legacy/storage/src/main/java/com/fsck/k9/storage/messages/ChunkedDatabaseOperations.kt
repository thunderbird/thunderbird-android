package com.fsck.k9.storage.messages

internal fun <T> performChunkedOperation(
    arguments: Collection<T>,
    argumentTransformation: (T) -> String,
    chunkSize: Int = 500,
    operation: (selectionSet: String, selectionArguments: Array<String>) -> Unit,
) {
    require(arguments.isNotEmpty()) { "'arguments' must not be empty" }
    require(chunkSize in 1..1000) { "'chunkSize' needs to be in 1..1000" }

    arguments.asSequence()
        .map(argumentTransformation)
        .chunked(chunkSize)
        .forEach { selectionArguments ->
            val selectionSet = selectionArguments.indices
                .joinToString(separator = ",", prefix = "IN (", postfix = ")") { "?" }

            operation(selectionSet, selectionArguments.toTypedArray())
        }
}
