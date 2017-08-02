package io.rsbot.net.codec.game;

import io.rsbot.net.MessageList;
import io.rsbot.net.NioClient;

import java.io.IOException;
import java.nio.channels.SocketChannel;

/**
 * A model that decodes incoming data into Runescape messages.
 *
 * @author lare96 <http://github.com/lare96>
 */
public interface MessageDecoder {

    /**
     * Decode incoming data sent from a server.
     *
     * @param client The client.
     * @param channel The socket channel.
     * @param out A list holding decoded messages.
     */
    void decode(NioClient client, SocketChannel channel, MessageList out) throws IOException;

    /**
     * Handle a decoded message.
     *
     * @param msg The message to handle.
     */
    default void handleMessage(Object msg) throws IOException {
       /* By default, do nothing. */
    }
}
