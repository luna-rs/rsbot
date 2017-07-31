package io.rsbot.net;

import io.rsbot.RsBot;
import io.rsbot.RsBotGroup;
import io.rsbot.net.codec.IsaacPair;
import io.rsbot.net.msg.RsBotMessage;

import java.io.IOException;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicReference;

import static io.rsbot.net.NioClientState.REGISTERED;
import static io.rsbot.net.NioClientState.LOGGED_IN;
import static io.rsbot.net.NioClientState.LOGGED_OUT;

/**
 * A model representing an asynchronous input/output channel.
 *
 * @author lare96 <http://github.org/lare96>
 */
public final class NioClient {

    /**
     * A queue of pending messages.
     */
    private final Queue<RsBotMessage> encodeQueue = new ConcurrentLinkedQueue<>();

    /**
     * The bot instance.
     */
    private final RsBot bot;

    /**
     * The login future.
     */
    private final LoginPromise loginPromise;

    /**
     * The event selector.
     */
    private final Selector selector;

    /**
     * The socket channel.
     */
    private final SocketChannel channel;

    /**
     * The current connection state.
     */
    private final AtomicReference<NioClientState> state = new AtomicReference<>(REGISTERED);

    /**
     * The ISAAC encryption and decryption pairs.
     */
    private Optional<IsaacPair> isaacPair = Optional.empty();

    /**
     * The selection key associated with this channel.
     */
    private Optional<SelectionKey> key = Optional.empty();

    /**
     * Creates a new {@link NioClient}.
     *
     * @param bot The bot instance.
     * @param loginPromise The login future.
     * @param selector The event selector.
     */
    public NioClient(RsBot bot, LoginPromise loginPromise, Selector selector) throws IOException {
        this.bot = bot;
        this.loginPromise = loginPromise;
        this.selector = selector;
        channel = SocketChannel.open();
    }

    /**
     * Connects the underlying channel to a server.
     */
    public void connect() throws IOException {
        channel.configureBlocking(false);

        SelectionKey newKey = channel.register(selector, SelectionKey.OP_CONNECT);
        newKey.attach(this);
        key = Optional.of(newKey);

        RsBotGroup group = bot.getGroup();
        channel.setOption(StandardSocketOptions.TCP_NODELAY, true);
        channel.connect(group.getConnectAddress());
    }

    /**
     * Writes a message to the underlying channel.
     */
    public void write(Object msg) {
        if (!channel.isOpen() || !channel.isConnected()) {
            return;
        }

        try {
            if (msg instanceof ByteBuffer) {
                writeBuffer((ByteBuffer) msg);
            } else if (msg instanceof RsBotMessage) {
                writeMsg((RsBotMessage) msg);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Writes an NIO buffer message.
     */
    private void writeBuffer(ByteBuffer buf) throws IOException {
        /* NIO messages are flushed right away. */
        buf.flip();
        channel.write(buf);
    }

    /**
     * Writes a Runescape message.
     */
    private void writeMsg(RsBotMessage msg) throws IOException {
        if (state.get() == LOGGED_IN && key.isPresent()) {
            /* Runescape messages are buffered. */
            encodeQueue.add(msg);
            key.get().interestOps(SelectionKey.OP_WRITE);
        }
    }

    /**
     * Closes the underlying channel.
     */
    public void close() throws IOException {
        if (channel.isOpen()) {
            channel.close();
            key.ifPresent(SelectionKey::cancel);
            state.set(LOGGED_OUT);
            bot.getGroup().get(bot.getUsername());
        }
    }

    /**
     * Returns {@code true} if this channel is open.
     */
    public boolean isActive() {
        return channel.isOpen();
    }

    /**
     * Sets the networking state.
     */
    public void setState(NioClientState newState) {
        state.set(newState);
    }

    /**
     * Returns the networking state.
     */
    public NioClientState getState() {
        return state.get();
    }

    /**
     * @return A queue of pending messages.
     */
    Queue<RsBotMessage> getEncodeQueue() {
        return encodeQueue;
    }

    /**
     * @return The underlying bot.
     */
    public RsBot getBot() {
        return bot;
    }

    /**
     * @return The login future.
     */
    LoginPromise getLoginPromise() {
        return loginPromise;
    }
}
