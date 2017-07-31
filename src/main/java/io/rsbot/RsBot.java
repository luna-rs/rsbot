package io.rsbot;

import io.rsbot.net.RsBotChannel;
import io.rsbot.net.LoginPromise;
import io.rsbot.util.StringUtils;

import java.io.IOException;
import java.util.Objects;

import static java.util.Objects.requireNonNull;

/**
 * A model representing a single bot. Every active bot is assigned to a group.
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
    private final RsBotChannel channel;

    /**
     * The login future.
     */
    private final LoginPromise loginPromise = new LoginPromise(this);

    /**
     * Creates a new {@link RsBot}.
     *
     * @param group The group this bot belongs to.
     * @param username The username.
     * @param password The password.
     */
    RsBot(RsBotGroup group, String username, String password) throws IOException {
        this.group = requireNonNull(group);
        this.username = requireNonNull(username);
        this.password = requireNonNull(password);
        usernameHash = StringUtils.encodeBase37(username);
        channel = new RsBotChannel(this, loginPromise, group.getSelector());
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
     * Connects this bot to a server.
     */
    protected void login() throws IOException {
        channel.connect();
    }

    /**
     * Removes this bot from its current group.
     */
    public void logout() throws IOException {
        group.logout(username);
    }

    /**
     * Writes {@code msg} to the underlying channel.
     */
    public void write(Object msg) {
        channel.write(msg);
    }

    /**
     * Returns {@code true} if this bot is logged in.
     */
    public boolean isLoggedIn() {
        return channel.isActive();
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

    /**
     * @return The login future.
     */
    LoginPromise getLoginPromise() {
        return loginPromise;
    }
}
