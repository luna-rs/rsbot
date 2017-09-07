package io.rsbot.net;

import io.rsbot.RsBot;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.logging.Logger;

/**
 * A Future model containing listeners to be applied at the end of login.
 *
 * @author lare96 <http://github.com/lare96>
 */
public final class LoginFuture implements Future<Boolean> {

    /**
     * A make-shift type alias.
     */
    public interface LoginListener extends Consumer<RsBot> {
    }

    /**
     * The logger.
     */
    private static final Logger LOGGER = Logger.getGlobal();

    /**
     * The bot instance.
     */
    private final RsBot bot;

    /**
     * A synchronization barrier.
     */
    private final CountDownLatch barrier = new CountDownLatch(1);

    /**
     * A queue of listeners.
     */
    private final Queue<LoginListener> listeners = new ConcurrentLinkedQueue<>();

    /**
     * Creates a new {@link LoginFuture}.
     *
     * @param bot The bot instance.
     */
    public LoginFuture(RsBot bot) {
        this.bot = bot;
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        LOGGER.warning("Cancellations not supported for 'LoginFuture'.");
        return false;
    }

    @Override
    public boolean isCancelled() {
        return false;
    }

    @Override
    public boolean isDone() {
        return barrier.getCount() == 0;
    }

    @Override
    public Boolean get() throws InterruptedException, ExecutionException {
        barrier.await();
        return bot.isLoggedIn();
    }

    @Override
    public Boolean get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        barrier.await(timeout, unit);
        return bot.isLoggedIn();
    }

    /**
     * Adds a listener.
     *
     * @param listener The listener.
     */
    public void addListener(LoginListener listener) {
        listeners.add(listener);
    }

    /**
     * Notify the backing queue of listeners.
     */
    void notifyListeners() {
        barrier.countDown();

        for (; ; ) {
            LoginListener listener = listeners.poll();
            if (listener == null) {
                break;
            }
            listener.accept(bot);
        }
    }

    /**
     * @return A synchronization barrier.
     */
    public CountDownLatch getBarrier() {
        return barrier;
    }
}
