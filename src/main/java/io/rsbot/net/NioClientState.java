package io.rsbot.net;

/**
 * An enumerated type representing networking states.
 *
 * @author lare96 <http://github.org/lare96>
 */
public enum NioClientState {

    /**
     * The socket channel has been registered with the selector.
     */
    REGISTERED,

    /**
     * The bot is undergoing the login protocol.
     */
    LOGGING_IN,

    /**
     * The bot is logged in and performing game input/output.
     */
    LOGGED_IN,

    /**
     * The bot is logged out.
     */
    LOGGED_OUT
}
