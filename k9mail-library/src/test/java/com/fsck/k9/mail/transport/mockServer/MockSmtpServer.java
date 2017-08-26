package com.fsck.k9.mail.transport.mockServer;


import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Deque;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

import android.annotation.SuppressLint;

import com.fsck.k9.mail.helpers.KeyStoreProvider;
import com.jcraft.jzlib.JZlib;
import com.jcraft.jzlib.ZOutputStream;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import okio.BufferedSink;
import okio.BufferedSource;
import okio.Okio;
import org.apache.commons.io.IOUtils;


@SuppressLint("NewApi")
public class MockSmtpServer {
    private static final byte[] CRLF = { '\r', '\n' };


    private final Deque<SmtpInteraction> interactions = new ConcurrentLinkedDeque<>();
    private final CountDownLatch waitForConnectionClosed = new CountDownLatch(1);
    private final CountDownLatch waitForAllExpectedCommands = new CountDownLatch(1);
    private final KeyStoreProvider keyStoreProvider;
    private final Logger logger;

    private MockServerThread mockServerThread;
    private String host;
    private int port;


    public MockSmtpServer() {
        this(KeyStoreProvider.getInstance(), new DefaultLogger());
    }

    public MockSmtpServer(KeyStoreProvider keyStoreProvider, Logger logger) {
        this.keyStoreProvider = keyStoreProvider;
        this.logger = logger;
    }

    public void output(String response) {
        checkServerNotRunning();
        interactions.add(new CannedResponse(response));
    }

    public void expect(String command) {
        checkServerNotRunning();
        interactions.add(new ExpectedCommand(command));
    }

    public void closeConnection() {
        checkServerNotRunning();
        interactions.add(new CloseConnection());
    }

    public void start() throws IOException {
        checkServerNotRunning();

        InetAddress localAddress = InetAddress.getByName(null);
        ServerSocket serverSocket = new ServerSocket(0, 1, localAddress);
        InetSocketAddress localSocketAddress = (InetSocketAddress) serverSocket.getLocalSocketAddress();
        host = localSocketAddress.getHostString();
        port = serverSocket.getLocalPort();

        mockServerThread = new MockServerThread(serverSocket, interactions, waitForConnectionClosed,
                waitForAllExpectedCommands, logger, keyStoreProvider);
        mockServerThread.start();
    }

    public void shutdown() {
        checkServerRunning();

        mockServerThread.shouldStop();
        waitForMockServerThread();
    }

    private void waitForMockServerThread() {
        try {
            mockServerThread.join(500L);
        } catch (InterruptedException ignored) {
        }
    }

    public String getHost() {
        checkServerRunning();

        return host;
    }

    public int getPort() {
        checkServerRunning();

        return port;
    }

    public void waitForInteractionToComplete() {
        checkServerRunning();

        try {
            waitForAllExpectedCommands.await(1000L, TimeUnit.MILLISECONDS);
        } catch (InterruptedException ignored) {
        }
    }

    public void verifyInteractionCompleted() {
        shutdown();

        if (!interactions.isEmpty()) {
            throw new AssertionError("Interactions left: " + interactions.size());
        }

        UnexpectedCommandException unexpectedCommandException = mockServerThread.getUnexpectedCommandException();
        if (unexpectedCommandException != null) {
            throw new AssertionError(unexpectedCommandException.getMessage(), unexpectedCommandException);
        }
    }

    public void verifyConnectionNeverCreated() {
        checkServerRunning();
        if (mockServerThread.clientConnectionCreated()) {
            throw new AssertionError("Connection created when it shouldn't have been");
        }

    }

    public void verifyConnectionStillOpen() {
        checkServerRunning();

        if (mockServerThread.isClientConnectionClosed()) {
            throw new AssertionError("Connection closed when it shouldn't be");
        }
    }

    public void verifyConnectionClosed() {
        checkServerRunning();

        try {
            waitForConnectionClosed.await(300L, TimeUnit.MILLISECONDS);
        } catch (InterruptedException ignored) {
        }

        if (!mockServerThread.isClientConnectionClosed()) {
            throw new AssertionError("Connection open when is shouldn't be");
        }
    }

    private void checkServerRunning() {
        if (mockServerThread == null) {
            throw new IllegalStateException("Server was never started");
        }
    }

    private void checkServerNotRunning() {
        if (mockServerThread != null) {
            throw new IllegalStateException("Server was already started");
        }
    }


    public interface Logger {
        void log(String message);

        void log(String format, Object... args);
    }

    private interface SmtpInteraction {
    }

    private static class ExpectedCommand implements SmtpInteraction {
        private final String command;


        public ExpectedCommand(String command) {
            this.command = command;
        }

        public String getCommand() {
            return command;
        }
    }

    private static class CannedResponse implements SmtpInteraction {
        private final String response;


        public CannedResponse(String response) {
            this.response = response;
        }

        public String getResponse() {
            return response;
        }
    }

    private static class CloseConnection implements SmtpInteraction {
    }

    private static class UnexpectedCommandException extends Exception {
        public UnexpectedCommandException(String expectedCommand, String receivedCommand) {
            super("Expected <" + expectedCommand + ">, but received <" + receivedCommand + ">");
        }
    }

    private static class MockServerThread extends Thread {
        private final ServerSocket serverSocket;
        private final Deque<SmtpInteraction> interactions;
        private final CountDownLatch waitForConnectionClosed;
        private final CountDownLatch waitForAllExpectedCommands;
        private final Logger logger;
        private final KeyStoreProvider keyStoreProvider;

        private volatile boolean shouldStop = false;
        private volatile Socket clientSocket;

        private BufferedSource input;
        private BufferedSink output;
        private volatile UnexpectedCommandException unexpectedCommandException;


        public MockServerThread(ServerSocket serverSocket, Deque<SmtpInteraction> interactions,
                CountDownLatch waitForConnectionClosed, CountDownLatch waitForAllExpectedCommands, Logger logger,
                KeyStoreProvider keyStoreProvider) {
            super("MockSmtpServer");
            this.serverSocket = serverSocket;
            this.interactions = interactions;
            this.waitForConnectionClosed = waitForConnectionClosed;
            this.waitForAllExpectedCommands = waitForAllExpectedCommands;
            this.logger = logger;
            this.keyStoreProvider = keyStoreProvider;
        }

        @Override
        public void run() {
            String hostAddress = serverSocket.getInetAddress().getHostAddress();
            int port = serverSocket.getLocalPort();
            logger.log("Listening on %s:%d", hostAddress, port);

            Socket socket = null;
            try {
                socket = acceptConnectionAndCloseServerSocket();
                clientSocket = socket;

                String remoteHostAddress = socket.getInetAddress().getHostAddress();
                int remotePort = socket.getPort();
                logger.log("Accepted connection from %s:%d", remoteHostAddress, remotePort);

                input = Okio.buffer(Okio.source(socket));
                output = Okio.buffer(Okio.sink(socket));

                while (!shouldStop && !interactions.isEmpty()) {
                    handleInteractions(socket);
                }

                waitForAllExpectedCommands.countDown();

                while (!shouldStop) {
                    readAdditionalCommands();
                }

                waitForConnectionClosed.countDown();
            } catch (UnexpectedCommandException e) {
                unexpectedCommandException = e;
            } catch (IOException e) {
                if (!shouldStop) {
                    logger.log("Exception: %s", e);
                }
            } catch (KeyStoreException | CertificateException | UnrecoverableKeyException |
                    NoSuchAlgorithmException | KeyManagementException e) {
                throw new RuntimeException(e);
            }

            IOUtils.closeQuietly(socket);

            logger.log("Exiting");
        }

        private void handleInteractions(Socket socket) throws IOException, KeyStoreException,
                NoSuchAlgorithmException, CertificateException, UnrecoverableKeyException, KeyManagementException,
                UnexpectedCommandException {

            SmtpInteraction interaction = interactions.pop();
            if (interaction instanceof ExpectedCommand) {
                readExpectedCommand((ExpectedCommand) interaction);
            } else if (interaction instanceof CannedResponse) {
                writeCannedResponse((CannedResponse) interaction);
            } else if (interaction instanceof CloseConnection) {
                clientSocket.close();
            }
        }

        private void readExpectedCommand(ExpectedCommand expectedCommand) throws IOException,
                UnexpectedCommandException {

            String command = input.readUtf8Line();
            if (command == null) {
                throw new EOFException();
            }

            logger.log("C: %s", command);

            String expected = expectedCommand.getCommand();
            if (!command.equals(expected)) {
                logger.log("EXPECTED: %s", expected);
                logger.log("ACTUAL: %s", command);
                throw new UnexpectedCommandException(expected, command);
            }
        }

        private void writeCannedResponse(CannedResponse cannedResponse) throws IOException {
            String response = cannedResponse.getResponse();
            logger.log("S: %s", response);

            output.writeUtf8(response);
            output.write(CRLF);
            output.flush();
        }

        private void enableCompression(Socket socket) throws IOException {
            InputStream inputStream = new InflaterInputStream(socket.getInputStream(), new Inflater(true));
            input = Okio.buffer(Okio.source(inputStream));

            ZOutputStream outputStream = new ZOutputStream(socket.getOutputStream(), JZlib.Z_BEST_SPEED, true);
            outputStream.setFlushMode(JZlib.Z_PARTIAL_FLUSH);
            output = Okio.buffer(Okio.sink(outputStream));
        }

        private void upgradeToTls(Socket socket) throws KeyStoreException, IOException, NoSuchAlgorithmException,
                CertificateException, UnrecoverableKeyException, KeyManagementException {

            KeyStore keyStore = keyStoreProvider.getKeyStore();

            String defaultAlgorithm = KeyManagerFactory.getDefaultAlgorithm();
            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(defaultAlgorithm);
            keyManagerFactory.init(keyStore, keyStoreProvider.getPassword());

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(keyManagerFactory.getKeyManagers(), null, null);
            SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

            SSLSocket sslSocket = (SSLSocket) sslSocketFactory.createSocket(
                    socket, socket.getInetAddress().getHostAddress(), socket.getPort(), true);
            sslSocket.setUseClientMode(false);
            sslSocket.startHandshake();

            input = Okio.buffer(Okio.source(sslSocket.getInputStream()));
            output = Okio.buffer(Okio.sink(sslSocket.getOutputStream()));
        }

        private void readAdditionalCommands() throws IOException {
            String command = input.readUtf8Line();
            if (command == null) {
                throw new EOFException();
            }

            logger.log("Received additional command: %s", command);
        }

        private Socket acceptConnectionAndCloseServerSocket() throws IOException {
            Socket socket = serverSocket.accept();
            serverSocket.close();

            return socket;
        }

        public void shouldStop() {
            shouldStop = true;

            IOUtils.closeQuietly(clientSocket);
        }

        public boolean clientConnectionCreated() {
            return clientSocket != null;
        }

        public boolean isClientConnectionClosed() {
            return clientSocket.isClosed();
        }

        public UnexpectedCommandException getUnexpectedCommandException() {
            return unexpectedCommandException;
        }
    }

    private static class DefaultLogger implements Logger {
        @Override
        public void log(String message) {
            System.out.println("MockSmtpServer: " + message);
        }

        @Override
        public void log(String format, Object... args) {
            log(String.format(format, args));
        }
    }
}
