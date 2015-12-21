package com.fsck.k9.mail.filter;


import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;


/**
 * Further encode a quoted-printable stream into a safer format for signed email.
 *
 * @see <a href="http://tools.ietf.org/html/rfc2015">RFC-2015</a>
 */
public class SignSafeOutputStream extends FilterOutputStream {
    private static final byte[] ESCAPED_SPACE = new byte[] { '=', '2', '0' };
    private static final int DEFAULT_BUFFER_SIZE = 1024;


    private State state = State.cr_FROM;
    private final byte[] outBuffer;
    private int outputIndex;

    private boolean closed = false;


    public SignSafeOutputStream(OutputStream out) {
        super(out);
        outBuffer = new byte[DEFAULT_BUFFER_SIZE];
    }

    public void encode(byte next) throws IOException {
        State nextState = state.nextState(next);
        if (nextState == State.SPACE_FROM) {
            state = State.INIT;
            writeToBuffer(ESCAPED_SPACE[0]);
            writeToBuffer(ESCAPED_SPACE[1]);
            writeToBuffer(ESCAPED_SPACE[2]);
        } else {
            state = nextState;
            writeToBuffer(next);
        }
    }

    private void writeToBuffer(byte next) throws IOException {
        outBuffer[outputIndex++] = next;
        if (outputIndex >= outBuffer.length) {
            flushOutput();
        }
    }

    void flushOutput() throws IOException {
        if (outputIndex < outBuffer.length) {
            out.write(outBuffer, 0, outputIndex);
        } else {
            out.write(outBuffer);
        }
        outputIndex = 0;
    }

    @Override
    public void write(int b) throws IOException {
        if (closed) {
            throw new IOException("Stream has been closed");
        }
        encode((byte) b);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        if (closed) {
            throw new IOException("Stream has been closed");
        }
        for (int inputIndex = off; inputIndex < len + off; inputIndex++) {
            encode(b[inputIndex]);
        }
    }

    @Override
    public void flush() throws IOException {
        flushOutput();
        out.flush();
    }

    @Override
    public void close() throws IOException {
        if (closed) {
            return;
        }

        try {
            flush();
        } finally {
            closed = true;
        }
    }

    enum State {
        INIT {
            @Override
            public State nextState(int b) {
                switch (b) {
                    case '\r':
                        return lf_FROM;
                    default:
                        return INIT;

                }
            }
        },
        lf_FROM {
            @Override
            public State nextState(int b) {
                switch (b) {
                    case '\n':
                        return cr_FROM;
                    case '\r':
                        return lf_FROM;
                    default:
                        return INIT;

                }
            }
        },
        cr_FROM {
            @Override
            public State nextState(int b) {
                switch (b) {
                    case 'F':
                        return F_FROM;
                    case '\r':
                        return lf_FROM;
                    default:
                        return INIT;

                }
            }
        },
        F_FROM {
            @Override
            public State nextState(int b) {
                switch (b) {
                    case 'r':
                        return R_FROM;
                    case '\r':
                        return lf_FROM;
                    default:
                        return INIT;

                }
            }
        },
        R_FROM {
            @Override
            public State nextState(int b) {
                switch (b) {
                    case 'o':
                        return O_FROM;
                    case '\r':
                        return lf_FROM;
                    default:
                        return INIT;

                }
            }
        },
        O_FROM {
            @Override
            public State nextState(int b) {
                switch (b) {
                    case 'm':
                        return M_FROM;
                    case '\r':
                        return lf_FROM;
                    default:
                        return INIT;

                }
            }
        },
        M_FROM {
            @Override
            public State nextState(int b) {
                switch (b) {
                    case ' ':
                        return SPACE_FROM;
                    case '\r':
                        return lf_FROM;
                    default:
                        return INIT;

                }
            }

        },
        SPACE_FROM {
            @Override
            public State nextState(int b) {
                switch (b) {
                    case '\r':
                        return lf_FROM;
                    default:
                        return INIT;

                }
            }

        };

        public abstract State nextState(int b);
    }
}


