package io.rsbot;

import io.rsbot.net.RsBotChannel;
import io.rsbot.util.StringUtils;

import java.util.Objects;

import static java.util.Objects.requireNonNull;

/**
 * A model representing a single bot.
 *
 * @author lare96 <http://github.com/lare96>
 */
public final class RsBot {

    /**
     * The group this bot belongs to.
     */
    private final RsBotGroup group;

    /**
     * The username.
     */
    private final String username;

    /**
     * The password.
     */
    private final String password;

    /**
     * The username hash.
     */
    private final long usernameHash;

    /**
     * The input/output channel.
     */
    private final RsBotChannel channel = new RsBotChannel(this);

    /**
     * Creates a new {@link RsBot}.
     *
     * @param group The group this bot belongs to.
     * @param username The username.
     * @param password The password.
     */
    RsBot(RsBotGroup group, String username, String password) {
        this.group = requireNonNull(group);
        this.username = requireNonNull(username);
        this.password = requireNonNull(password);
        usernameHash = StringUtils.encodeBase37(username);
    }

    @Override
    public int hashCode() {
        return Objects.hash(username);
    }

    @Override
    public String toString() {
        return "RsBot{username= " + username + "}";
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof RsBot) {
            RsBot bot = (RsBot) obj;
            return usernameHash == bot.usernameHash;
        }
        return false;
    }

    /**
     * Removes this bot from its current group.
     */
    public void disconnect() {
        group.remove(username);
    }

    /**
     * Writes {@code msg} to the underlying channel.
     */
    public void write(Object msg) {
        channel.write(msg);
    }

    /**
     * @return The group this bot belongs to.
     */
    public RsBotGroup getGroup() {
        return group;
    }

    /**
     * @return The username.
     */
    public String getUsername() {
        return username;
    }

    /**
     * @return The password.
     */
    public String getPassword() {
        return password;
    }

    /**
     * @return The username hash.
     */
    public long getUsernameHash() {
        return usernameHash;
    }

    /**
     * @return The input/output channel.
     */
    RsBotChannel getChannel() {
        return channel;
    }
}
