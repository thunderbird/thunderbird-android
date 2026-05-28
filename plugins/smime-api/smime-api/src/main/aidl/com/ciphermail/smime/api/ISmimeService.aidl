package com.ciphermail.smime.api;

/**
 * S/MIME companion service API.
 *
 * All operation semantics travel as Intent extras (see SmimeApi for constants).
 * Bulk data (message bytes) streams through ParcelFileDescriptor pipes so that
 * large messages never cross the IPC boundary as in-memory byte arrays.
 *
 * Callers (e.g. Thunderbird):
 *   1. Call createOutputPipe(pipeId) to get the write end of the output pipe.
 *   2. Call execute(intent, inputPipe, pipeId) with the action Intent and the
 *      read end of the input pipe.
 *   3. Read from the output pipe while execute() is running (it blocks until done).
 *   4. Inspect the returned Intent for RESULT_CODE and result Parcelables.
 */
interface ISmimeService {

    /**
     * Create the write end of an output pipe identified by pipeId.
     * The caller reads from the corresponding read end while the service writes
     * the processed message bytes to this write end.
     */
    ParcelFileDescriptor createOutputPipe(in int pipeId);

    /**
     * Execute an S/MIME operation.
     *
     * @param data    Intent carrying action string and all EXTRA_* parameters.
     * @param input   Read end of the input pipe (raw MIME bytes), or null for
     *                actions that need no message input (e.g. ACTION_GET_CERTIFICATES).
     * @param pipeId  Matches the pipeId passed to createOutputPipe().
     * @return        Intent carrying RESULT_CODE and result Parcelables.
     */
    Intent execute(in Intent data, in ParcelFileDescriptor input, int pipeId);
}
