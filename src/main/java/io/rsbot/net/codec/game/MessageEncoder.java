package io.rsbot.net.codec.game;

import io.rsbot.net.NioClient;
import io.rsbot.net.codec.NioBuffer;

import java.io.IOException;

/**
 * A model that encodes outgoing Runescape messages into data.
 *
 * @author lare96 <http://github.com/lare96>
 */
public interface MessageEncoder {

    /**
     * Encode outgoing data to be sent by a client.
     *
     * @param client The client.
     * @param msg The message to encode.
     */
    void encode(NioClient client, NioBuffer msg) throws IOException;
}
