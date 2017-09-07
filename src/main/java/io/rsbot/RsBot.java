package io.rsbot;

import io.rsbot.net.LoginFuture;
import io.rsbot.net.NioClient;
import io.rsbot.net.NioClientState;
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
     * The input/output client.
     */
    private final NioClient client;

    /**
     * The login notifier.
     */
    private final LoginFuture loginFuture = new LoginFuture(this);

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
        client = new NioClient(this, loginFuture, group.getSelector());
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
        client.connect();
    }

    /**
     * Removes this bot from its current group.
     */
    public void logout() throws IOException {
        group.logout(username);
    }

    /**
     * Writes {@code msg} to the underlying client.
     */
    public void write(Object msg) {
        client.write(msg);
    }

    /**
     * Returns {@code true} if this bot is logged in.
     */
    public boolean isLoggedIn() {
        return client.getState() == NioClientState.LOGGED_IN;
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
     * @return The input/output client.
     */
    NioClient getClient() {
        return client;
    }

    /**
     * @return The login future.
     */
    LoginFuture getLoginFuture() {
        if(loginFuture.isDone()) {
            throw new IllegalStateException("Login has already been completed");
        }
        return loginFuture;
    }
}
