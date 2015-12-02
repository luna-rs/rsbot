package org.jbot;

/**
 * An enumerated type representing all of the states a {@link LocalBot} can be in.
 *
 * @author lare96 <http://github.org/lare96>
 */
public enum LocalBotState {

    /**
     * A {@link LocalBot} has just been registered, a asynchronous connection is being executed.
     */
    CONNECTING,

    /**
     * A {@link LocalBot} has connected to the server and is undergoing the first stage of the login protocol where a simple
     * request handshake is exchanged
     */
    INITIAL_LOGIN_REQUEST,

    /**
     * A {@link LocalBot} is undergoing the second and largest stage of the login protocol where all of the information about
     * the {@code LocalBot} is sent.
     */
    INITIAL_LOGIN_RESPONSE,

    /**
     * A {@link LocalBot} is undergoing the third and final stage of the login protocol where the response code is sent from
     * the server.
     */
    FINAL_LOGIN_RESPONSE,

    /**
     * A {@link LocalBot} successfully underwent all stages of the login protocol and is ready to perform game I/O. On the
     * server the bot will be logged in.
     */
    LOGGED_IN,

    /**
     * A {@link LocalBot} has been removed from it's {@link LocalBotGroup} and is unable to perform any I/O. On the server
     * the bot will be logged out.
     */
    LOGGED_OUT
}
