package io.rsbot.net;

import io.rsbot.RsBot;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

/**
 * A model containing listeners to be applied at the end of login.
 *
 * @author lare96 <http://github.com/lare96>
 */
public final class LoginPromise {

    /**
     * The bot instance.
     */
    private final RsBot bot;

    /**
     * The queue of listeners.
     */
    private final Queue<Consumer<RsBot>> listeners = new ConcurrentLinkedQueue<>();

    /**
     * Creates a new {@link LoginPromise}.
     *
     * @param bot The bot instance.
     */
    public LoginPromise(RsBot bot) {
        this.bot = bot;
    }

    /**
     * Adds a listener.
     *
     * @param eventListener The listener.
     */
    public void addListener(Consumer<RsBot> eventListener) {
        listeners.add(eventListener);
    }

    /**
     * Apply the queue of listeners.
     */
    void apply() {
        for (; ; ) {
            Consumer<RsBot> eventListener = listeners.poll();
            if (eventListener == null) {
                break;
            }
            eventListener.accept(bot);
        }
    }
}
