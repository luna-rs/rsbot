package org.jbot;

import org.jbot.net.ByteMessage;
import org.jbot.net.LocalBotReactor;
import org.jbot.net.codec.MessageEncoderProvider;
import org.jbot.util.JBotIsaac;
import org.jbot.util.NioUtils;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.concurrent.atomic.AtomicReference;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Objects.requireNonNull;

/**
 * An in-house {@code LocalBot} that is preparing for login or that has already logged onto the user's server. Every single
 * {@code LocalBot} belongs to a {@link LocalBotGroup}. The {@link MessageEncoderProvider} getter can provide access to
 * encoded functions such as walking and talking, and the {@code LocalBotGroup} getter can provide the user with access to
 * the {@code LocalBotGroup} that this {@code LocalBot} belongs to.
 *
 * @author lare96 <http://github.org/lare96>
 */
public final class LocalBot {

    /**
     * The {@link LocalBotGroup} that this {@code LocalBot} belongs to.
     */
    private final LocalBotGroup botGroup;

    /**
     * The username of this {@code LocalBot}.
     */
    private final String username;

    /**
     * The password of this {@code LocalBot}.
     */
    private final String password;

    /**
     * A {@link SocketChannel} that will be used by this {@code LocalBot} for input/output.
     */
    private final SocketChannel socket;

    /**
     * The {@link SelectionKey} assigned to the underlying {@link SocketChannel}.
     */
    private final SelectionKey selectionKey;

    /**
     * The primary {@link ByteBuffer} that will be used to read messages.
     */
    private final ByteBuffer readBuf = ByteBuffer.allocateDirect(256);

    /**
     * The current {@link LocalBotState} that this {@code LocalBot} is in.
     */
    private final AtomicReference<LocalBotState> state = new AtomicReference<>(LocalBotState.CONNECTING);

    /**
     * A {@link JBotIsaac} that will encode messages.
     */
    private JBotIsaac encryptor;

    /**
     * Creates a new {@link LocalBot}.
     *
     * @param botGroup The {@link LocalBotGroup} that this {@code LocalBot} belongs to.
     * @param username The username of this {@code LocalBot}.
     * @param password The password of this {@code LocalBot}.
     */
    public LocalBot(LocalBotGroup botGroup, String username, String password) {
        this.botGroup = botGroup;
        this.username = requireNonNull(username);
        this.password = requireNonNull(password);

        LocalBotReactor reactor = botGroup.getReactor();

        socket = NioUtils.openSocketChannel(botGroup);
        selectionKey = NioUtils.registerSocketChannel(botGroup, socket, reactor, SelectionKey.OP_READ);
    }

    /**
     * Attempts to asynchronously connect and then later log in this {@code LocalBot}.
     *
     * @throws IOException If an error occurs while connecting this {@code LocalBot}.
     */
    protected void login() throws IOException {
        selectionKey.attach(this);

        socket.configureBlocking(false);
        socket.setOption(StandardSocketOptions.TCP_NODELAY, true);
        socket.connect(new InetSocketAddress("127.0.0.1", 43594));

        state.set(LocalBotState.INITIAL_LOGIN_REQUEST);
    }

    /**
     * Attempts to asynchronously close the underlying {@link SocketChannel} and cancel the {@link SelectionKey}.
     */
    protected void logout() {
        try {
            socket.close();
            selectionKey.cancel();

            state.set(LocalBotState.LOGGED_OUT);
        } catch (IOException e) {
            botGroup.getExceptionHandler().onBotException(this, e);
        }
    }

    /**
     * Writes a {@code msg} to this backing {@link SocketChannel}.
     *
     * @param msg The message to write.
     */
    public void write(ByteMessage msg) {
        checkState(socket.isOpen(), "socket closed");

        try {
            msg.getBuffer().flip();
            socket.write(msg.getBuffer());
        } catch (IOException e) {
            botGroup.getExceptionHandler().onBotException(this, e);
        }
    }

    /**
     * @return The {@link MessageEncoderProvider} that will provide message encoding.
     */
    public MessageEncoderProvider messages() {
        return botGroup.getMessageProvider();
    }

    /**
     * @return The {@link LocalBotGroup} that this {@code LocalBot} belongs to.
     */
    public LocalBotGroup getBotGroup() {
        return botGroup;
    }

    /**
     * @return The username of this {@code LocalBot}.
     */
    public String getUsername() {
        return username;
    }

    /**
     * @return The password of this {@code LocalBot}.
     */
    public String getPassword() {
        return password;
    }

    /**
     * @return A {@link SocketChannel} that will be used by this {@code LocalBot} for input/output.
     */
    public SocketChannel getSocket() {
        return socket;
    }

    /**
     * @return The {@link SelectionKey} assigned to the underlying {@link SocketChannel}.
     */
    public SelectionKey getSelectionKey() {
        return selectionKey;
    }

    /**
     * @return The current {@link LocalBotState} that this {@code LocalBot} is in.
     */
    public AtomicReference<LocalBotState> getState() {
        return state;
    }

    /**
     * @return The primary {@link ByteBuffer} that will be used to read messages.
     */
    public ByteBuffer getReadBuf() {
        return readBuf;
    }

    /**
     * @return A {@link JBotIsaac} that will encode messages.
     */
    public JBotIsaac getEncryptor() {
        return encryptor;
    }

    /**
     * Sets the value for {@link #encryptor}.
     */
    public void setEncryptor(JBotIsaac encryptor) {
        this.encryptor = encryptor;
    }
}
