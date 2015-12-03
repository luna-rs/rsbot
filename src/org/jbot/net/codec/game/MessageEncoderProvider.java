package org.jbot.net.codec.game;

import org.jbot.LocalBot;
import org.jbot.net.msg.JBotMessage;

/**
 * A provider that holds functionality for encoding messages that are sent to the server.
 *
 * @author lare96 <http://github.org/lare96>
 */
public interface MessageEncoderProvider {

    /**
     * Encode {@code toEncode} to send the message to the server.
     *
     * @param localBot The {@link LocalBot} this message is being encoded for.
     * @param toEncode The {@link JBotMessage} that will be encoded.
     * @throws Exception If any errors occur while encoding.
     */
    public void encode(LocalBot localBot, JBotMessage toEncode) throws Exception;
}
