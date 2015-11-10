package com.fsck.k9.mail.filter;

import org.apache.james.mime4j.codec.QuotedPrintableOutputStream;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

/**
 * Further encode a quoted-printable stream into a safer format for signed email.
 * @see <a href="http://tools.ietf.org/html/rfc2015">RFC-2015</a>
 */
public class SignSafeOutputStream extends FilterOutputStream {
    private ByteBuffer buffer = ByteBuffer.allocate(1);

    private State state = State.cr_FROM;

    public SignSafeOutputStream(OutputStream out){
        super(out);
    }

    public static FilterOutputStream newInstance(OutputStream out){
        return new QuotedPrintableOutputStream(new SignSafeOutputStream(out), false);
    }

    private static final byte[] ESCAPE = new byte[]{'=','2','0'};

    @Override
    public void write(int oneByte) throws IOException {
        State nextState = state.nextState(oneByte);
        if (nextState == State.SPACE_FROM){
            state = State.INIT;
            out.write(ESCAPE);
        } else if (nextState == State.lf_SPACE){
            buffer.clear();
            state = State.lf_FROM;
            out.write(ESCAPE);
            out.write(oneByte);
        } else if (nextState == State.SPACE) {
            writeBuffer();
            buffer.put((byte)32);
            state = nextState;
        } else {
            writeBuffer();
            state = nextState;
            out.write(oneByte);
        }
    }

    private void writeBuffer() throws IOException {
        buffer.flip();
        if (buffer.hasRemaining()) {
            out.write(buffer.get());
        }
        buffer.clear();
    }

    @Override
    public void close() throws IOException {
        writeBuffer();
        out.close();
    }

    enum State {
        INIT {
            @Override
            public State nextState(int b) {
                switch (b) {
                    case '\r':
                        return lf_FROM;
                    case ' ':
                        return SPACE;
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
                    case ' ':
                        return SPACE;
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
                    case ' ':
                        return SPACE;
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
                    case ' ':
                        return SPACE;
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
                    case ' ':
                        return SPACE;
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
                    case ' ':
                        return SPACE;
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
                        return lf_SPACE;
                    case 'F':
                        return F_FROM;
                    case ' ':
                        return SPACE;
                    default:
                        return INIT;

                }
            }

        },
        SPACE {
            @Override
            public State nextState(int b) {
                switch (b) {
                    case '\r':
                        return lf_SPACE;
                    case 'F':
                        return F_FROM;
                    case ' ':
                        return SPACE;
                    default:
                        return INIT;

                }
            }
        },
        lf_SPACE {
            @Override
            public State nextState(int b) {
                return INIT;
            }
        };

        public abstract State nextState(int b);
    }
}


