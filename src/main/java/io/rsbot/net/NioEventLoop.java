package io.rsbot.net;

import io.rsbot.RsBotGroup;
import io.rsbot.net.codec.game.MessageDecoder;
import io.rsbot.net.codec.game.MessageEncoder;
import io.rsbot.net.codec.login.LoginEncoder;
import io.rsbot.net.msg.RsBotMessage;

import java.io.IOException;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Optional;
import java.util.Queue;
import java.util.logging.Logger;

/**
 * A single-threaded reactor that handles events for all bots pertaining to a group.
 *
 * @author lare96 <http://github.com/lare96>
 */
public final class NioEventLoop extends Thread {

    /**
     * The utility logger.
     */
    private static final Logger LOGGER = Logger.getGlobal();

    /**
     * A list of decoded messages.
     */
    private final MessageList msgList = new MessageList();

    /**
     * The group of bots.
     */
    private final RsBotGroup group;

    /**
     * The event selector.
     */
    private Optional<Selector> eventSelector = Optional.empty();

    /**
     * Creates a new {@link NioEventLoop}.
     *
     * @param group The group of bots.
     */
    public NioEventLoop(RsBotGroup group) {
        this.group = group;
    }

    @Override
    public void run() {
        setName("RsBotIOThread");

        try {
            Selector selector = getSelector();
            for (; ; ) {
                selector.select();
                Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();

                while (iterator.hasNext()) {
                    SelectionKey key = iterator.next();
                    handle(key);
                    iterator.remove();
                }
            }
        } catch (ClosedSelectorException e) {
            LOGGER.info("The event loop for Runescape bots has shut down.");
        } catch (Exception e) {
            LOGGER.severe("The event loop thread has encountered an error.");
            e.printStackTrace();
        } finally {
            logoutAll();
        }
    }

    /**
     * Handles selected networking events.
     */
    private void handle(SelectionKey key) throws IOException {
        if (key.isValid()) {
            if (key.isConnectable()) {
                handleConnect(key);
            } else if (key.isReadable()) {
                handleRead(key);
            } else if (key.isWritable()) {
                handleWrite(key);
            }
        }
    }

    /**
     * Handles the {@code OP_CONNECT} event.
     */
    private void handleConnect(SelectionKey key) throws IOException {
        NioClient client = (NioClient) key.attachment();

        SocketChannel channel = (SocketChannel) key.channel();
        if (channel.finishConnect()) {
            client.setState(NioClientState.LOGGING_IN);
            LoginEncoder loginEncoder = group.getLoginEncoder();
            loginEncoder.encode(client);

            client.getLoginPromise().apply();
            key.interestOps(SelectionKey.OP_READ);
        }
    }

    /**
     * Handles the {@code OP_READ} event.
     */
    private void handleRead(SelectionKey key) throws IOException {
        MessageDecoder msgDecoder = group.getMessageDecoder();
        msgDecoder.decode((NioClient) key.attachment(), (SocketChannel) key.channel(), msgList);

        Iterator<Object> it = msgList.mutableIterator();
        while (it.hasNext()) {
            Object msg = it.next();
            msgDecoder.handleMessage(msg);
            it.remove();
        }
    }

    /**
     * Handles the {@code OP_WRITE} event.
     */
    private void handleWrite(SelectionKey key) throws IOException {
        NioClient client = (NioClient) key.attachment();
        Queue<RsBotMessage> encodeQueue = client.getEncodeQueue();
        for (; ; ) {
            RsBotMessage msg = encodeQueue.poll();
            if (msg == null) {
                break;
            }
            MessageEncoder msgEncoder = group.getMessageEncoder();
            client.write(msgEncoder.encode(client, msg));
        }
        key.interestOps(SelectionKey.OP_READ);
    }

    /**
     * Disconnects all bots associated with this event loop.
     */
    private void logoutAll() {
        try {
            group.logoutAll();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * @return The event selector. Creates one if needed.
     */
    public Selector getSelector() throws IOException {
        if (!eventSelector.isPresent()) {
            eventSelector = Optional.of(Selector.open());
        }
        return eventSelector.get();
    }
}
