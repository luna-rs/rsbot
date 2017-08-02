package io.rsbot.net.codec.game;

import io.rsbot.net.MessageList;
import io.rsbot.net.NioClient;
import io.rsbot.net.codec.NioBuffer;
import io.rsbot.net.msg.RsBotMessage;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.SocketChannel;

/**
 * A model that decodes {@code 317} Runescape messages.
 *
 * @author lare96 <http://github.com/lare96>
 */
public class Rs317MessageDecoder implements MessageDecoder {

    /**
     * An array of packet lengths.
     */
    private static final int[] PACKET_LENGTHS = {0, 0, 0, 0, 6, 0, 0, 0, 4, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            1, 0, 0, 0, 0, 0, 0, 0, 0, 0, -2, 4, 3, 0, 0, 0, 0, 0, 0, 0, 5, 0, 0, 6, 0, 0, 9, 0, 0, -2, 0, 0, 0, 0, 0,
            0, -2, 1, 0, 0, 2, -2, 0, 0, 0, 0, 6, 3, 2, 4, 2, 4, 0, 0, 0, 4, 0, -2, 0, 0, 7, 2, 0, 6, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 2, 0, 1, 0, 2, 0, 0, -1, 4, 1, 0, 0, 0, 1, 0, 0, 0, 2, 0, 0, 15, 0, 0, 0, 4, 4, 0, 0, 0, -2, 0, 0,
            0, 0, 0, 0, 0, 6, 0, 0, 0, 0, 0, 0, 0, 2, 0, 0, 0, 0, 14, 0, 0, 0, 4, 0, 0, 0, 0, 3, 0, 0, 0, 4, 0, 0, 0,
            2, 0, 6, 0, 0, 0, 0, 3, 0, 0, 5, 0, 10, 6, 0, 0, 0, 0, 0, 0, 0, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -1, 0, 0,
            0, 4, 0, 0, 0, 0, 0, 3, 0, 2, 0, 0, 0, 0, 0, -2, 7, 0, 0, 2, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 8, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 2, -2, 0, 0, 0, 0, 6, 0, 4, 3, 0, 0, 0, -1, 6, 0, 0};

    /**
     * The current opcode.
     */
    private int opcode = -1;

    /**
     * The current length.
     */
    private int length = 0;

    @Override
    public void decode(NioClient client, SocketChannel channel, MessageList out) throws IOException {
        ByteBuffer buffer = client.getReadBuffer();

        int available = channel.read(buffer);
        if (available == 0) {
            return;
        }
        buffer.flip();
        while (buffer.hasRemaining()) {
            if (opcode == -1) {
                opcode = buffer.get() & 0xff;
                if (client.hasIsaac()) {
                    opcode = opcode - client.getDecryptor().getKey() & 0xff;
                }
                length = PACKET_LENGTHS[opcode];
                available--;
            }
            if (length == -1) {
                if (available > 0) {
                    length = buffer.get() & 0xff;
                    available--;
                } else {
                    break;
                }
            }
            if (length == -2) {
                if (available > 1) {
                    ByteOrder oldOrder = buffer.order();

                    buffer.order(ByteOrder.LITTLE_ENDIAN);
                    length = buffer.getShort() & 0xff;
                    buffer.order(oldOrder);

                    available -= 2;
                } else {
                    break;
                }
            }
            if (available < length) {
               break;
            }
            int startPos = buffer.position();
            int endPos = startPos + length;

            NioBuffer payload = NioBuffer.create(length);
            for (int index = startPos; index < endPos; index++) {
                payload.put(buffer.get(index));
            }
            buffer.position(endPos);

            out.add(new RsBotMessage(opcode, length, payload));

            opcode = -1;
            length = 0;
        }
        opcode = -1;
        length = 0;
        buffer.clear();
    }
}
