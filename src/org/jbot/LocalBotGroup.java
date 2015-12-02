package org.jbot;

import org.jbot.net.LocalBotReactor;
import org.jbot.net.codec.LoginEncoderProvider;
import org.jbot.net.codec.MessageEncoderProvider;
import org.jbot.net.codec.Rs317LoginEncoderProvider;
import org.jbot.net.codec.Rs317MessageEncoderProvider;
import org.jbot.util.JBotExceptionHandler;
import org.jbot.util.LoggerJBotExceptionHandler;
import org.jbot.util.RsaKeyPair;

import java.io.IOException;
import java.util.*;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Objects.requireNonNull;

/**
 * An {@link Iterable} implementation representing a group of {@link LocalBot}s. Typically, only one {@code LocalBotGroup}
 * instance is needed in a server although the user is free to create more. One should avoid creating too many instances
 * though in order to keep resource usage and complexity to a minimum. All I/O operations are done asynchronously and
 * as a result only one thread is needed per {@code LocalBotGroup}.
 *
 * @author lare96 <http://github.org/lare96>
 */
public final class LocalBotGroup implements Iterable<LocalBot> {

    /**
     * A class that utilizes the builder pattern for {@link LocalBotGroup} instances.
     */
    public static final class LocalBotGroupBuilder {

        /**
         * The message encoder implementation for {@link LocalBot}s in this {@code LocalBotGroup}.
         */
        private MessageEncoderProvider messageProvider = new Rs317MessageEncoderProvider();

        /**
         * The login encoder implementation for {@link LocalBot}s in this {@code LocalBotGroup}.
         */
        private LoginEncoderProvider loginProvider = new Rs317LoginEncoderProvider();

        /**
         * The exception handler implementation for {@link LocalBot}s in this {@code LocalBotGroup}.
         */
        private JBotExceptionHandler exceptionHandler = new LoggerJBotExceptionHandler();

        /**
         * The RSA modulus and exponent values that will be used to login, {@code null} if there is no RSA key-pair.
         */
        private RsaKeyPair rsaKeyPair;

        /**
         * A chaining method that sets the message encoder. {@code null} is not permitted.
         */
        public LocalBotGroupBuilder messageProvider(MessageEncoderProvider messageProvider) {
            this.messageProvider = requireNonNull(messageProvider);
            return this;
        }

        /**
         * A chaining method that sets the login encoder. {@code null} is not permitted.
         */
        public LocalBotGroupBuilder loginProvider(LoginEncoderProvider loginProvider) {
            this.loginProvider = requireNonNull(loginProvider);
            return this;
        }

        /**
         * A chaining method that sets the exception handler. {@code null} is not permitted.
         */
        public LocalBotGroupBuilder exceptionHandler(JBotExceptionHandler exceptionHandler) {
            this.exceptionHandler = requireNonNull(exceptionHandler);
            return this;
        }

        /**
         * A chaining method that sets the RSA key-pair.
         */
        public LocalBotGroupBuilder rsa(RsaKeyPair rsaKeyPair) {
            this.rsaKeyPair = rsaKeyPair;
            return this;
        }

        /**
         * @return A new {@link LocalBotGroup} with the specified settings.
         */
        public LocalBotGroup build() {
            return new LocalBotGroup(messageProvider, loginProvider, exceptionHandler, rsaKeyPair);
        }
    }

    /**
     * An {@link Iterator} implementation for {@link LocalBotGroup}s, supports all of the operations of a typical {@code
     * Iterator}.
     */
    private static final class LocalBotGroupIterator implements Iterator<LocalBot> {

        /**
         * The {@code LocalBotGroup} that this {@code Iterator} is for.
         */
        private final LocalBotGroup botGroup;

        /**
         * The last {@code LocalBot} that was returned by {@code next()}.
         */
        private LocalBot cachedNext;

        /**
         * Creates a new {@link LocalBotGroupIterator}.
         *
         * @param botGroup The {@code LocalBotGroup} that this {@code Iterator} is for.
         */
        private LocalBotGroupIterator(LocalBotGroup botGroup) {
            this.botGroup = botGroup;
        }

        @Override
        public boolean hasNext() {
            Iterator<LocalBot> $it = botGroup.localBots.values().iterator();
            boolean hasNext = $it.hasNext();

            if (!hasNext) {
                cachedNext = null;
            }
            return hasNext;
        }

        @Override
        public LocalBot next() {
            Iterator<LocalBot> $it = botGroup.localBots.values().iterator();
            cachedNext = $it.next();
            return cachedNext;
        }

        @Override
        public void remove() {
            Iterator<LocalBot> $it = botGroup.localBots.values().iterator();
            $it.remove();
            cachedNext.logout();
        }
    }

    /**
     * A {@link Map} of usernames mapped to {@link LocalBot} instances.
     */
    private final Map<String, LocalBot> localBots = new LinkedHashMap<>();

    /**
     * The message encoder implementation for {@link LocalBot}s in this {@code LocalBotGroup}.
     */
    private final MessageEncoderProvider messageProvider;

    /**
     * The login encoder implementation for {@link LocalBot}s in this {@code LocalBotGroup}.
     */
    private final LoginEncoderProvider loginProvider;

    /**
     * The exception handler implementation for {@link LocalBot}s in this {@code LocalBotGroup}.
     */
    private final JBotExceptionHandler exceptionHandler;

    /**
     * The RSA modulus and exponent values that will be used to login, {@code null} if there is no RSA key-pair.
     */
    private final RsaKeyPair rsaKeyPair;

    /**
     * The NIO reactor that will handle all input/output events for {@link LocalBot}s.
     */
    private final LocalBotReactor reactor = new LocalBotReactor(this);

    /**
     * Creates a new {@link LocalBotGroup}.
     *
     * @param messageProvider The message encoder implementation for {@link LocalBot}s in this {@code LocalBotGroup}.
     * @param loginProvider The login encoder implementation for {@link LocalBot}s in this {@code LocalBotGroup}.
     * @param exceptionHandler The exception handler implementation for {@link LocalBot}s in this {@code LocalBotGroup}.
     * @param rsaKeyPair The RSA modulus and exponent values that will be used to login, {@code null} if there is no RSA
     * key-pair.
     */
    public LocalBotGroup(MessageEncoderProvider messageProvider, LoginEncoderProvider loginProvider, JBotExceptionHandler exceptionHandler, RsaKeyPair rsaKeyPair) {
        this.messageProvider = messageProvider;
        this.loginProvider = loginProvider;
        this.exceptionHandler = exceptionHandler;
        this.rsaKeyPair = rsaKeyPair;
    }

    @Override
    public Iterator<LocalBot> iterator() {
        return new LocalBotGroupIterator(this);
    }

    @Override
    public Spliterator<LocalBot> spliterator() {
        return Spliterators
            .spliterator(new LocalBotGroupIterator(this), localBots.size(), Spliterator.NONNULL | Spliterator.ORDERED);
    }

    /**
     * Adds a new {@link LocalBot} to this {@code LocalBotGroup} and performs an asynchronous login for it. Will throw an
     * {@link IllegalStateException} if this {@code LocalBotGroup} already contains a {@code LocalBot} with the same username
     * as {@code username}.
     *
     * @param username The username of the {@link LocalBot}.
     * @param password The password of the {@link LocalBot}.
     * @return An instance of the newly created {@link LocalBot}.
     */
    public LocalBot add(String username, String password) {
        LocalBot localBot = new LocalBot(this, username, password);

        checkState(localBots.putIfAbsent(username, localBot) == null,
            "LocalBotGroup already contains LocalBot with this name");

        try {
            localBot.login();
        } catch (IOException e) {
            exceptionHandler.onBotException(localBot, e);
        }
        return localBot;
    }

    /**
     * Removes an existing {@link LocalBot} from this {@code LocalBotGroup}.
     *
     * @param username The username of the {@link LocalBot} to remove.
     * @return {@code true} if the {@link LocalBot} was successfully removed, {@code false} otherwise.
     */
    public boolean remove(String username) {
        LocalBot localBot = localBots.remove(username);
        if (localBot != null) {
            localBot.logout();
            return true;
        }
        return false;
    }

    /**
     * Removes an existing {@link LocalBot} from this {@code LocalBotGroup}. Functions in exactly the same way as {@code
     * remove(String)}.
     *
     * @param localBot The {@link LocalBot} to remove.
     * @return {@code true} if the {@link LocalBot} was successfully removed, {@code false} otherwise.
     */
    public boolean remove(LocalBot localBot) {
        return remove(localBot.getUsername());
    }

    /**
     * Determines if a {@link LocalBot} with {@code username} exists within this {@code LocalBotGroup}.
     *
     * @param username The username to check for.
     * @return {@code true} if a {@link LocalBot} with {@code username} exists, {@code false} otherwise.
     */
    public boolean exists(String username) {
        return localBots.containsKey(username);
    }

    /**
     * Determines if a {@link LocalBot} instance exists within this {@code LocalBotGroup}.
     *
     * @param localBot The {@link LocalBot} instance to check for.
     * @return {@code true} if a {@link LocalBot} with {@code username} exists, {@code false} otherwise.
     */
    public boolean exists(LocalBot localBot) {
        return exists(localBot.getUsername());
    }

    /**
     * Retrieves a {@link LocalBot} instance by its name. Will throw an {@link IllegalStateException} if no {@code LocalBot}
     * with that name exists.
     *
     * @param username The username of the {@link LocalBot} to retrieve.
     * @return An instance of the {@link LocalBot}.
     */
    public LocalBot get(String username) {
        LocalBot localBot = localBots.get(username);

        checkState(localBot != null, "LocalBot with this name does not exist");
        return localBot;
    }

    /**
     * @return The message encoder implementation for {@link LocalBot}s in this {@code LocalBotGroup}.
     */
    public MessageEncoderProvider getMessageProvider() {
        return messageProvider;
    }

    /**
     * @return The login encoder implementation for {@link LocalBot}s in this {@code LocalBotGroup}.
     */
    public LoginEncoderProvider getLoginProvider() {
        return loginProvider;
    }

    /**
     * @return The exception handler implementation for {@link LocalBot}s in this {@code LocalBotGroup}.
     */
    public JBotExceptionHandler getExceptionHandler() {
        return exceptionHandler;
    }

    /**
     * @return The RSA modulus and exponent values that will be used to login, {@code null} if there is no RSA key-pair.
     */
    public RsaKeyPair getRsaKeyPair() {
        return rsaKeyPair;
    }

    /**
     * @return The NIO reactor that will handle all input/output events for {@link LocalBot}s.
     */
    public LocalBotReactor getReactor() {
        return reactor;
    }
}
