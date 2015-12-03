package org.jbot.net.msg;

import org.jbot.LocalBot;
import org.jbot.net.codec.JBotBuffer;

/**
 * An outbound message writer for {@link LocalBot}s. Will build {@link JBotBuffer}s which are later converted into {@link
 * JBotMessage}s.
 *
 * @author lare96 <http://github.org/lare96>
 */
public abstract class JBotMessageWriter {

    /**
     * Writes data for this message into a created {@link JBotBuffer}.
     *
     * @param localBot The {@link LocalBot} that it will be written for.
     * @return The buffer containing the data.
     */
    public abstract JBotBuffer write(LocalBot localBot);

    /**
     * Converts the {@link JBotBuffer} returned by {@code write(LocalBot)} to a {@link JBotMessage}.
     *
     * @param localBot The {@link LocalBot} that it will be written for.
     * @return The successfully converted message.
     */
    public JBotMessage toJBotMessage(LocalBot localBot) {
        JBotBuffer buf = write(localBot);
        return new JBotMessage(buf.getBuffer().get(0), buf.getBuffer().position(), buf);
    }
}
