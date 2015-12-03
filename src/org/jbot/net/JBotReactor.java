package org.jbot.net;

import com.google.common.collect.Iterators;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.jbot.LocalBot;
import org.jbot.LocalBotGroup;
import org.jbot.LocalBotState;
import org.jbot.util.NioUtils;

import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * An asynchronous NIO reactor that handles all networking events for {@link LocalBot}s. If the thread that this reactor runs
 * is ever interrupted it will logout all bots within the {@link LocalBotGroup} and the thread will exit, rendering the
 * {@code LocalBotGroup} useless.
 *
 * @author lare96 <http://github.org/lare96>
 */
public final class JBotReactor implements Runnable {

    /**
     * An {@link ExecutorService} that will run this NIO reactor.
     */
    private final ExecutorService reactorPool = Executors
        .newSingleThreadExecutor(new ThreadFactoryBuilder().setNameFormat("JBotIOThread").build());

    /**
     * The {@link LocalBotGroup} this reactor is dedicated to.
     */
    private final LocalBotGroup botGroup;

    /**
     * The {@link Selector} that will select networking events for {@link LocalBot}s.
     */
    private final Selector selector;

    /**
     * Creates a new {@link JBotReactor}.
     *
     * @param botGroup The {@link LocalBotGroup} this reactor is dedicated to.
     */
    public JBotReactor(LocalBotGroup botGroup) {
        this.botGroup = botGroup;

        selector = NioUtils.openSelector(botGroup);
        reactorPool.execute(this);
    }

    @Override
    public void run() {
        try {
            while (!Thread.interrupted()) {
                try {
                    doAsyncSelect();
                } catch (Throwable t) {
                    botGroup.getExceptionHandler().onBotGroupException(botGroup, t);
                }
            }
            Iterators.removeIf(botGroup.iterator(), it -> true);
            reactorPool.execute(this);

            throw new IllegalStateException("JBotIOThread was interrupted! Attempting to restart thread...");
        } catch (Throwable t) {
            botGroup.getExceptionHandler().onBotGroupException(botGroup, t);
        }
    }

    /**
     * Performs an asynchronous selection operation for networking events, and determines if data can be read from these
     * events.
     *
     * @throws Exception If any errors occur during non-blocking select.
     */
    private void doAsyncSelect() throws Exception {
        selector.selectNow();
        Iterator<SelectionKey> selectedKeys = selector.selectedKeys().iterator();

        while (selectedKeys.hasNext()) {
            SelectionKey key = selectedKeys.next();
            if (key.isValid() && key.isReadable()) {
                readMessages(key);
            }
            selectedKeys.remove();
        }
    }

    /**
     * Decodes either login or game messages from the server. Game messages are currently read and discarded, this may change
     * in the future.
     *
     * @param key The {@link SelectionKey} that will be used for its attachment.
     * @throws Exception If any errors occur while decoding data.
     */
    private void readMessages(SelectionKey key) throws Exception {
        LocalBot localBot = (LocalBot) key.attachment();

        if (localBot.getSocket().read(localBot.getReadBuf()) == -1) {
            return;
        }
        localBot.getReadBuf().flip();
        while (localBot.getReadBuf().hasRemaining()) {
            if (localBot.getState().get() != LocalBotState.LOGGED_IN) {
                botGroup.getLoginProvider().encodeLogin(localBot);
                break;
            }
            localBot.getReadBuf().get(new byte[localBot.getReadBuf().remaining()]);
        }
    }

    /**
     * @return The {@link Selector} that will select networking events for {@link LocalBot}s.
     */
    public Selector getSelector() {
        return selector;
    }
}
