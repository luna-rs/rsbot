package io.rsbot.net.codec.game;

import io.rsbot.net.NioClient;
import io.rsbot.net.codec.Isaac;
import io.rsbot.net.codec.NioBuffer;

import java.io.IOException;

/**
 * A model that encodes {@code 317} Runescape messages.
 *
 * @author lare96 <http://github.com/lare96>
 */
public class Rs317MessageEncoder implements MessageEncoder {

    @Override
    public void encode(NioClient client, NioBuffer msg) throws IOException {
        Isaac encryptor = client.getEncryptor();
        byte[] raw = msg.getBuffer().array();
        raw[0] = (byte) (raw[0] + encryptor.getKey());

        client.write(msg);
    }
}
