package io.rsbot;

import io.rsbot.net.RsBotEventLoop;
import io.rsbot.net.RsBotChannel;
import io.rsbot.net.codec.RsaKeyPair;
import io.rsbot.net.codec.game.MessageDecoder;
import io.rsbot.net.codec.game.MessageEncoder;
import io.rsbot.net.codec.game.Rs317MessageDecoder;
import io.rsbot.net.codec.game.Rs317MessageEncoder;
import io.rsbot.net.codec.login.LoginEncoder;
import io.rsbot.net.codec.login.Rs317LoginEncoder;

import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.Objects.requireNonNull;

/**
 * A model representing a group of bots.
 *
 * @author lare96 <http://github.com/lare96>
 */
public final class RsBotGroup implements Iterable<RsBot> {

    /**
     * A builder used to create new {@link RsBotGroup} instances.
     */
    public static final class RsBotGroupBuilder {

        /**
         * The connection address (defaults to 127.0.0.1/43594).
         */
        private InetSocketAddress connectAddress = new InetSocketAddress("127.0.0.1", 43594);

        /**
         * The message encoder (defaults to {@code 317}).
         */
        private MessageEncoder messageEncoder = new Rs317MessageEncoder();

        /**
         * The message decoder (defaults to {@code 317}).
         */
        private MessageDecoder messageDecoder = new Rs317MessageDecoder();

        /**
         * The login encoder (defaults to {@code 317}).
         */
        private LoginEncoder loginEncoder = new Rs317LoginEncoder();

        /**
         * The public RSA modulus and exponent keys.
         */
        private Optional<RsaKeyPair> keyPair = Optional.empty();

        /**
         * Sets the connection address.
         */
        public RsBotGroupBuilder connectAddress(InetSocketAddress connectAddress) {
            this.connectAddress = requireNonNull(connectAddress);
            return this;
        }

        /**
         * Sets the message encoder.
         */
        public RsBotGroupBuilder messageEncoder(MessageEncoder encoder) {
            this.messageEncoder = requireNonNull(encoder);
            return this;
        }

        /**
         * Sets the message decoder.
         */
        public RsBotGroupBuilder messageDecoder(MessageDecoder decoder) {
            this.messageDecoder = requireNonNull(decoder);
            return this;
        }

        /**
         * Sets the login encoder.
         */
        public RsBotGroupBuilder loginEncoder(LoginEncoder encoder) {
            this.loginEncoder = requireNonNull(encoder);
            return this;
        }

        /**
         * Sets the public RSA modulus and exponent keys.
         */
        public RsBotGroupBuilder keyPair(RsaKeyPair keyPair) {
            this.keyPair = Optional.of(keyPair);
            return this;
        }

        /**
         * Returns a new {@link RsBotGroup} instance.
         */
        public RsBotGroup build() {
            return new RsBotGroup(connectAddress, messageEncoder, messageDecoder, loginEncoder, keyPair);
        }
    }

    /**
     * A concurrent map.
     */
    private final Map<String, RsBot> bots = new ConcurrentHashMap<>();

    /**
     * An event loop handling networking.
     */
    private final RsBotEventLoop eventLoop = new RsBotEventLoop(this);

    /**
     * The connection address.
     */
    private final InetSocketAddress connectAddress;

    /**
     * The message encoder.
     */
    private final MessageEncoder messageEncoder;

    /**
     * The message decoder.
     */
    private final MessageDecoder messageDecoder;

    /**
     * The login encoder.
     */
    private final LoginEncoder loginEncoder;

    /**
     * The RSA modulus and exponent keys.
     */
    private final Optional<RsaKeyPair> keyPair;

    /**
     * Creates a new {@link RsBotGroup}.
     *
     * @param connectAddress The connection address.
     * @param messageEncoder The message encoder.
     * @param messageDecoder The message decoder.
     * @param loginEncoder The login encoder.
     * @param keyPair The RSA modulus and exponent keys.
     */
    private RsBotGroup(InetSocketAddress connectAddress, MessageEncoder messageEncoder,
                       MessageDecoder messageDecoder, LoginEncoder loginEncoder, Optional<RsaKeyPair> keyPair) {
        this.connectAddress = connectAddress;
        this.messageEncoder = messageEncoder;
        this.messageDecoder = messageDecoder;
        this.loginEncoder = loginEncoder;
        this.keyPair = keyPair;
    }

    /**
     * The returned iterator is <strong>immutable</strong>.
     */
    @Override
    public Iterator<RsBot> iterator() {
        Collection<RsBot> immutable = Collections.unmodifiableCollection(bots.values());
        return immutable.iterator();
    }

    @Override
    public Spliterator<RsBot> spliterator() {
        return Spliterators.spliterator(iterator(), bots.size(), Spliterator.NONNULL | Spliterator.ORDERED |
                Spliterator.IMMUTABLE);
    }

    /**
     * Adds a new bot to this group.
     */
    public RsBot add(String username, String password) {
        if (!eventLoop.isAlive()) { /* Only start the reactor when needed! */
            eventLoop.start();
        }

        RsBot newBot = new RsBot(this, username, password);
        if (bots.putIfAbsent(username, newBot) != null) {
            throw new IllegalStateException("Group already contains bot with username: " + username);
        }
        return newBot;
    }

    /**
     * Removes an existing bot from this group.
     */
    public RsBot remove(String username) {
        RsBot removedBot = Optional.ofNullable(bots.remove(username)).
                orElseThrow(IllegalStateException::new);
        removedBot.getChannel().close();
        return removedBot;
    }

    /**
     * Removes all bots from this group.
     */
    public void removeAll() {
        Iterator<RsBot> iterator = bots.values().iterator();
        while (iterator.hasNext()) {
            RsBotChannel channel = iterator.next().getChannel();
            channel.close();
            iterator.remove();
        }
    }

    /**
     * Determines if a bot exists within this group.
     */
    public boolean contains(String username) {
        return bots.containsKey(username);
    }

    /**
     * Retrieves an instance of a bot from this group.
     */
    public Optional<RsBot> get(String username) {
        return Optional.ofNullable(bots.get(username));
    }

    /**
     * @return The connection address.
     */
    public InetSocketAddress getConnectAddress() {
        return connectAddress;
    }

    /**
     * @return The message encoder.
     */
    public MessageEncoder getMessageEncoder() {
        return messageEncoder;
    }

    /**
     * @return The message decoder.
     */
    public MessageDecoder getMessageDecoder() {
        return messageDecoder;
    }

    /**
     * @return The login encoder.
     */
    public LoginEncoder getLoginEncoder() {
        return loginEncoder;
    }

    /**
     * @return The public RSA modulus and exponent keys.
     */
    public Optional<RsaKeyPair> getKeyPair() {
        return keyPair;
    }
}
