package org.jbot.net.msg;

import org.jbot.net.codec.JBotBuffer;

/**
 * A message that acts as an inbound or outbound packet of data.
 *
 * @author lare96 <http://github.org/lare96>
 */
public final class JBotMessage {

    /**
     * The opcode of this message.
     */
    private final int opcode;

    /**
     * The size of this message.
     */
    private final int size;

    /**
     * The payload of this message.
     */
    private final JBotBuffer payload;

    /**
     * Creates a new {@link JBotMessage}.
     *
     * @param opcode The opcode of this message.
     * @param size The size of this message.
     * @param payload The payload of this message.
     */
    public JBotMessage(int opcode, int size, JBotBuffer payload) {
        this.opcode = opcode;
        this.size = size;
        this.payload = payload;
    }

    /**
     * @return The opcode of this message.
     */
    public int getOpcode() {
        return opcode;
    }

    /**
     * @return The size of this message.
     */
    public int getSize() {
        return size;
    }

    /**
     * @return The payload of this message.
     */
    public JBotBuffer getPayload() {
        return payload;
    }
}
