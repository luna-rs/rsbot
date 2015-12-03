package org.jbot.net.codec.login;

import org.jbot.LocalBot;

/**
 * A provider that holds functionality for encoding the login protocol.
 *
 * @author lare96 <http://github.org/lare96>
 */
public interface LoginEncoderProvider {

    /**
     * Encodes the login protocol for {@code localBot}.
     *
     * @param localBot The {@link LocalBot} this message is being encoded for.
     * @throws Exception If any errors occur while encoding.
     */
    public void encodeLogin(LocalBot localBot) throws Exception;
}
