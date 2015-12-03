package org.jbot.net.codec.game;

import org.jbot.LocalBot;
import org.jbot.net.codec.JBotIsaac;
import org.jbot.net.msg.JBotMessage;

import java.nio.ByteBuffer;

/**
 * A {@link MessageEncoderProvider} implementation that encodes all outgoing game messages. It is designed for the {@code
 * 317} protocol but should work on most servers.
 *
 * @author lare96 <http://github.org/lare96>
 */
public class Rs317MessageEncoderProvider implements MessageEncoderProvider {

    @Override
    public void encode(LocalBot localBot, JBotMessage toEncode) {
        JBotIsaac isaac = localBot.getEncryptor();
        ByteBuffer buf = toEncode.getPayload().getBuffer();

        buf.put(0, (byte) (buf.get(0) + isaac.getKey()));

        localBot.write(buf);
    }
}
