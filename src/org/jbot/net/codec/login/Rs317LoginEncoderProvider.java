package org.jbot.net.codec.login;

import org.jbot.LocalBot;
import org.jbot.LocalBotState;
import org.jbot.net.codec.JBotBuffer;
import org.jbot.net.codec.JBotIsaac;
import org.jbot.util.StringUtils;

import java.nio.ByteBuffer;
import java.util.concurrent.ThreadLocalRandom;

import static com.google.common.base.Preconditions.checkState;

/**
 * A {@link LoginEncoderProvider} implementation that encodes the login protocol. It is designed for the {@code 317} protocol
 * but should work on most servers.
 *
 * @author lare96 <http://github.org/lare96>
 */
public class Rs317LoginEncoderProvider implements LoginEncoderProvider {

    @Override
    public final void encodeLogin(LocalBot localBot) throws Exception {
        switch (localBot.getState().get()) {
        case INITIAL_LOGIN_REQUEST:
            initialLoginRequest(localBot);
            break;
        case INITIAL_LOGIN_RESPONSE:
            initialLoginResponse(localBot);
            break;
        case FINAL_LOGIN_RESPONSE:
            finalLoginResponse(localBot);
            break;
        }
    }

    /**
     * Sends the initial login request. Access modifier of protected so specific sections can be overridden.
     *
     * @param localBot The {@link LocalBot} that this is for.
     */
    protected void initialLoginRequest(LocalBot localBot) {
        JBotBuffer initRequest = JBotBuffer.create();

        initRequest.put(14);
        initRequest.put((int) (StringUtils.encodeBase37(localBot.getUsername()) >> 16 & 31L));

        localBot.write(initRequest);
    }

    /**
     * Receive the initial login response and send the secure block containing credentials as well as a block containing
     * client information. Access modifier of protected so specific sections can be overridden.
     *
     * @param localBot The {@link LocalBot} that this is for.
     */
    protected void initialLoginResponse(LocalBot localBot) {
        ByteBuffer readBuf = localBot.getReadBuf();

        if (readBuf.remaining() < 17) {
            readBuf.compact();
            return;
        }
        readBuf.getLong();

        int opcode = readBuf.get();
        checkState(opcode == 0, "invalid response opcode: " + opcode);

        JBotBuffer secureBlock = JBotBuffer.create();
        int[] seed = { ThreadLocalRandom.current().nextInt(), ThreadLocalRandom.current().nextInt(), readBuf.getInt(),
            readBuf.getInt() };
        secureBlock.put(10);
        for (int val : seed) {
            secureBlock.putInt(val);
        }
        secureBlock.putInt(455437);
        secureBlock.putString(localBot.getUsername());
        secureBlock.putString(localBot.getPassword());

        JBotBuffer clientInfo = JBotBuffer.create();
        clientInfo.put(16);
        clientInfo.put(secureBlock.getBuffer().position() + 40);
        clientInfo.put(1);
        clientInfo.put(317);
        clientInfo.put(0);

        secureBlock.encodeRSA(localBot.getBotGroup().getRsaKey());

        for (int i = 0; i < 9; i++) {
            clientInfo.putInt(ThreadLocalRandom.current().nextInt());
        }
        clientInfo.put(secureBlock.getBuffer().position() - 1);
        clientInfo.putBytes(secureBlock.getBuffer());

        localBot.setEncryptor(new JBotIsaac(seed));
        for (int i = 0; i < 4; i++) {
            seed[i] += 50;
        }
        // TODO: Set decryptor for message decoding

        localBot.write(clientInfo);
    }

    /**
     * Receive the final login response. Access modifier of protected so specific sections can be overridden.
     *
     * @param localBot The {@link LocalBot} that this is for.
     */
    protected void finalLoginResponse(LocalBot localBot) {
        ByteBuffer readBuf = localBot.getReadBuf();

        if (readBuf.remaining() < 1) {
            readBuf.compact();
            return;
        }
        int opcode = readBuf.get();
        checkState(opcode == 2, "invalid final response code: " + opcode);
        localBot.getState().set(LocalBotState.LOGGED_IN);
    }
}
