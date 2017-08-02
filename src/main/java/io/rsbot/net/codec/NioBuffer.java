package io.rsbot.net.codec;

import java.math.BigInteger;
import java.nio.ByteBuffer;

/**
 * A buffer tailored to the specifications of the RuneScape protocol.
 *
 * @author lare96 <http://github.com/lare96>
 */
public final class NioBuffer {

    /**
     * An array of bit masks.
     */
    public static final int[] BIT_MASK = new int[32];

    /**
     * The backing buffer.
     */
    private ByteBuffer buf;

    /**
     * The variable length buffer position.
     */
    private int varLengthIndex = 0;

    /**
     * The bit buffer position.
     */
    private int bitIndex = 0;

    /**
     * Creates a new {@link NioBuffer}.
     *
     * @param buf The backing buffer.
     */
    private NioBuffer(ByteBuffer buf) {
        this.buf = buf;
    }

    static {
        for (int i = 0; i < BIT_MASK.length; i++) {
            BIT_MASK[i] = (1 << i) - 1;
        }
    }

    /**
     * Creates a new {@link NioBuffer}.
     */
    public static NioBuffer create(ByteBuffer buf) {
        return new NioBuffer(buf);
    }

    /**
     * Creates a new {@link NioBuffer}.
     */
    public static NioBuffer create(int capacity) {
        return NioBuffer.create(ByteBuffer.allocate(capacity));
    }

    /**
     * Creates a new {@link NioBuffer}.
     */
    public static NioBuffer create() {
        return NioBuffer.create(128);
    }

    /**
     * Prepares the buffer for writing bits.
     */
    public void startBitAccess() {
        bitIndex = buf.position() * 8;
    }

    /**
     * Prepares the buffer for writing bytes.
     */
    public void endBitAccess() {
        buf.position((bitIndex + 7) / 8);
    }

    /**
     * Ensures this buffer can hold more bytes.
     */
    private void ensureSpace() {
        while ((buf.position() + 2) >= buf.capacity()) {
            int oldPosition = buf.position();
            byte[] oldBuffer = buf.array();
            int newLength = (buf.capacity() * 2);

            buf = ByteBuffer.allocate(newLength);

            buf.position(oldPosition);

            System.arraycopy(oldBuffer, 0, buf.array(), 0, oldBuffer.length);
        }
    }

    /**
     * Builds a new message header.
     *
     * @param opcode The opcode of the message.
     * @return This buffer.
     */
    public NioBuffer message(int opcode) {
        put(opcode);
        return this;
    }

    /**
     * Builds a new message header for a variable length message.
     *
     * @param opcode The opcode of the message.
     * @return This buffer.
     */
    public NioBuffer varMessage(int opcode) {
        message(opcode);
        varLengthIndex = buf.position();
        put(0);
        return this;
    }

    /**
     * Builds a new message header for a variable {@code short} length message.
     *
     * @param opcode The opcode of the message.
     * @return This buffer.
     */
    public NioBuffer varShortMessage(int opcode) {
        message(opcode);
        varLengthIndex = buf.position();
        putShort(0);
        return this;
    }

    /**
     * Finishes a variable message header.
     *
     * @return This buffer.
     */
    public NioBuffer endVarMessage() {
        buf.put(varLengthIndex, (byte) (buf.position() - varLengthIndex - 1));
        return this;
    }

    /**
     * Finishes a variable {@code short} message header.
     * 
     * @return This buffer.
     */
    public NioBuffer endVarShortMessage() {
        buf.putShort(varLengthIndex, (short) (buf.position() - varLengthIndex - 2));
        return this;
    }

    /**
     * Writes bytes into this buffer. Do not flip the buffer beforehand.
     *
     * @param from The buffer bytes will be written from.
     * @return This buffer.
     */
    public NioBuffer putBytes(ByteBuffer from) {
        for (int i = 0; i < from.position(); i++) {
            put(from.get(i));
        }
        return this;
    }

    /**
     * Writes bytes into this buffer.
     *
     * @param from The buffer bytes will be written from.
     * @return This buffer.
     */
    public NioBuffer putBytes(byte[] from, int size) {
        for (int i = 0; i < size; i++) {
            put(from[i]);
        }
        return this;
    }

    /**
     * Writes bytes into this buffer, in reverse.
     *
     * @param from The buffer bytes will be written from.
     * @return This buffer.
     */
    public NioBuffer putBytesReverse(byte[] from) {
        return putBytesReverse(from, NioByteType.NORMAL);
    }

    /**
     * Writes bytes into this buffer, in reverse.
     *
     * @param from The buffer bytes will be written from.
     * @param type The type of bytes to write.
     * @return This buffer.
     */
    public NioBuffer putBytesReverse(byte[] from, NioByteType type) {
        for (int i = from.length - 1; i >= 0; i--) {
            switch (type) {
                case A:
                    put(from[i] + 128);
                    break;
                case C:
                    put(-from[i]);
                    break;
                case S:
                    put(from[i] - 128);
                    break;
                case NORMAL:
                    break;
            }
        }
        return this;
    }

    /**
     * Writes bits to this buffer.
     *
     * @param amount The bit amount.
     * @param value The bit value.
     * @return This buffer.
     */
    public NioBuffer putBits(int amount, int value) {
        if (amount < 0 || amount > 32)
            throw new IllegalArgumentException("Number of bits must be between 1 and 32 inclusive.");
        int bytePos = bitIndex >> 3;
        int bitOffset = 8 - (bitIndex & 7);
        bitIndex = bitIndex + amount;
        int requiredSpace = bytePos - buf.position() + 1;
        requiredSpace += (amount + 7) / 8;
        if (buf.remaining() < requiredSpace) {
            ByteBuffer old = buf;
            buf = ByteBuffer.allocate(old.capacity() + requiredSpace);
            buf.put(old);
        }
        for (; amount > bitOffset; bitOffset = 8) {
            byte tmp = buf.get(bytePos);
            tmp &= ~BIT_MASK[bitOffset];
            tmp |= (value >> (amount - bitOffset)) & BIT_MASK[bitOffset];
            buf.put(bytePos++, tmp);
            amount -= bitOffset;
        }
        if (amount == bitOffset) {
            byte tmp = buf.get(bytePos);
            tmp &= ~BIT_MASK[bitOffset];
            tmp |= value & BIT_MASK[bitOffset];
            buf.put(bytePos, tmp);
        } else {
            byte tmp = buf.get(bytePos);
            tmp &= ~(BIT_MASK[amount] << (bitOffset - amount));
            tmp |= (value & BIT_MASK[amount]) << (bitOffset - amount);
            buf.put(bytePos, tmp);
        }
        return this;
    }

    /**
     * Writes a boolean bit flag.
     *
     * @param flag The bit flag.
     * @return This buffer.
     */
    public NioBuffer putBit(boolean flag) {
        putBits(1, flag ? 1 : 0);
        return this;
    }

    /**
     * Writes a value as a {@code byte}.
     *
     * @param value The value to write.
     * @param type The byte transformation type.
     * @return This buffer.
     */
    public NioBuffer put(int value, NioByteType type) {
        ensureSpace();
        switch (type) {
            case A:
                value += 128;
                break;
            case C:
                value = -value;
                break;
            case S:
                value = 128 - value;
                break;
            case NORMAL:
                break;
        }
        buf.put((byte) value);
        return this;
    }

    /**
     * Writes a value as a normal {@code byte}.
     *
     * @param value The value to write.
     * @return This buffer.
     */
    public NioBuffer put(int value) {
        put(value, NioByteType.NORMAL);
        return this;
    }

    /**
     * Writes a value as a {@code short}.
     *
     * @param value The value to write.
     * @param type The byte transformation type
     * @param order The byte endianness type.
     * @return This buffer.
     * @throws UnsupportedOperationException If middle or inverse-middle value types are selected.
     */
    public NioBuffer putShort(int value, NioByteType type, NioByteOrder order) {
        switch (order) {
            case BIG:
                put(value >> 8);
                put(value, type);
                break;
            case MIDDLE:
                throw new UnsupportedOperationException("Middle-endian short is impossible.");
            case INVERSE_MIDDLE:
                throw new UnsupportedOperationException("Inversed-middle-endian short is impossible.");
            case LITTLE:
                put(value, type);
                put(value >> 8);
                break;
        }
        return this;
    }

    /**
     * Writes a value as a normal big-endian {@code short}.
     *
     * @param value The value to write.
     * @return This buffer.
     */
    public NioBuffer putShort(int value) {
        putShort(value, NioByteType.NORMAL, NioByteOrder.BIG);
        return this;
    }

    /**
     * Writes a value as a big-endian {@code short}.
     *
     * @param value The value to write.
     * @param type The byte transformation type
     * @return This buffer.
     */
    public NioBuffer putShort(int value, NioByteType type) {
        putShort(value, type, NioByteOrder.BIG);
        return this;
    }

    /**
     * Writes a value as a standard {@code short}.
     *
     * @param value The value to write.
     * @param order The byte endianness type.
     * @return This buffer.
     */
    public NioBuffer putShort(int value, NioByteOrder order) {
        putShort(value, NioByteType.NORMAL, order);
        return this;
    }

    /**
     * Writes a value as an {@code int}.
     *
     * @param value The value to write.
     * @param type The byte transformation type
     * @param order The byte endianness type.
     * @return This buffer.
     */
    public NioBuffer putInt(int value, NioByteType type, NioByteOrder order) {
        switch (order) {
            case BIG:
                put(value >> 24);
                put(value >> 16);
                put(value >> 8);
                put(value, type);
                break;
            case MIDDLE:
                put(value >> 8);
                put(value, type);
                put(value >> 24);
                put(value >> 16);
                break;
            case INVERSE_MIDDLE:
                put(value >> 16);
                put(value >> 24);
                put(value, type);
                put(value >> 8);
                break;
            case LITTLE:
                put(value, type);
                put(value >> 8);
                put(value >> 16);
                put(value >> 24);
                break;
        }
        return this;
    }

    /**
     * Writes a value as a standard big-endian {@code int}.
     *
     * @param value The value to write.
     * @return This buffer.
     */
    public NioBuffer putInt(int value) {
        putInt(value, NioByteType.NORMAL, NioByteOrder.BIG);
        return this;
    }

    /**
     * Writes a value as a big-endian {@code int}.
     *
     * @param value The value to write.
     * @param type The byte transformation type
     * @return This buffer.
     */
    public NioBuffer putInt(int value, NioByteType type) {
        putInt(value, type, NioByteOrder.BIG);
        return this;
    }

    /**
     * Writes a value as a standard {@code int}.
     *
     * @param value The value to write.
     * @param order The byte endianness type.
     * @return This buffer.
     */
    public NioBuffer putInt(int value, NioByteOrder order) {
        putInt(value, NioByteType.NORMAL, order);
        return this;
    }

    /**
     * Writes a value as a {@code long}.
     *
     * @param value The value to write.
     * @param type The byte transformation type
     * @param order The byte endianness type.
     * @return This buffer.
     * @throws UnsupportedOperationException If middle or inverse-middle value types are selected.
     */
    public NioBuffer putLong(long value, NioByteType type, NioByteOrder order) {
        switch (order) {
            case BIG:
                put((int) (value >> 56));
                put((int) (value >> 48));
                put((int) (value >> 40));
                put((int) (value >> 32));
                put((int) (value >> 24));
                put((int) (value >> 16));
                put((int) (value >> 8));
                put((int) value, type);
                break;
            case MIDDLE:
                throw new UnsupportedOperationException("Middle-endian long is not implemented!");
            case INVERSE_MIDDLE:
                throw new UnsupportedOperationException("Inverse-middle-endian long is not implemented!");
            case LITTLE:
                put((int) value, type);
                put((int) (value >> 8));
                put((int) (value >> 16));
                put((int) (value >> 24));
                put((int) (value >> 32));
                put((int) (value >> 40));
                put((int) (value >> 48));
                put((int) (value >> 56));
                break;
        }
        return this;
    }

    /**
     * Writes a value as a standard big-endian {@code long}.
     *
     * @param value The value to write.
     * @return This buffer.
     */
    public NioBuffer putLong(long value) {
        putLong(value, NioByteType.NORMAL, NioByteOrder.BIG);
        return this;
    }

    /**
     * Writes a value as a big-endian {@code long}.
     *
     * @param value The value to write.
     * @param type The byte transformation type
     * @return This buffer.
     */
    public NioBuffer putLong(long value, NioByteType type) {
        putLong(value, type, NioByteOrder.BIG);
        return this;
    }

    /**
     * Writes a value as a standard {@code long}.
     *
     * @param value The value to write.
     * @param order The byte endianness type. to write.
     * @return This buffer.
     */
    public NioBuffer putLong(long value, NioByteOrder order) {
        putLong(value, NioByteType.NORMAL, order);
        return this;
    }

    /**
     * Writes a RuneScape {@code String} value.
     *
     * @param string The string to write.
     * @return This buffer.
     */
    public NioBuffer putString(String string) {
        for (byte value : string.getBytes()) {
            put(value);
        }
        put(10);
        return this;
    }

    /**
     * Reads a value as a {@code byte}.
     *
     * @param signed if the byte is signed.
     * @param type The byte transformation type
     * @return The value of the byte.
     */
    public int get(boolean signed, NioByteType type) {
        int value = buf.get();
        switch (type) {
            case A:
                value = value - 128;
                break;
            case C:
                value = -value;
                break;
            case S:
                value = 128 - value;
                break;
            case NORMAL:
                break;
        }
        return signed ? value : value & 0xff;
    }

    /**
     * Reads a standard signed {@code byte}.
     *
     * @return The value of the byte.
     */
    public int get() {
        return get(true, NioByteType.NORMAL);
    }

    /**
     * Reads a standard {@code byte}.
     *
     * @param signed If the byte is signed.
     * @return The value of the byte.
     */
    public int get(boolean signed) {
        return get(signed, NioByteType.NORMAL);
    }

    /**
     * Reads a signed {@code byte}.
     *
     * @param type The byte transformation type
     * @return The value of the byte.
     */
    public int get(NioByteType type) {
        return get(true, type);
    }

    /**
     * Reads a {@code short} value.
     *
     * @param signed If the short is signed.
     * @param type The byte transformation type
     * @param order The byte endianness type.
     * @return The value of the short.
     * @throws UnsupportedOperationException if middle or inverse-middle value types are selected.
     */
    public int getShort(boolean signed, NioByteType type, NioByteOrder order) {
        int value = 0;
        switch (order) {
            case BIG:
                value |= get(false) << 8;
                value |= get(false, type);
                break;
            case MIDDLE:
                throw new UnsupportedOperationException("Middle-endian short is impossible!");
            case INVERSE_MIDDLE:
                throw new UnsupportedOperationException("Inverse-middle-endian short is impossible!");
            case LITTLE:
                value |= get(false, type);
                value |= get(false) << 8;
                break;
        }
        return signed ? value : value & 0xffff;
    }

    /**
     * Reads a standard signed big-endian {@code short}.
     *
     * @return The value of the short.
     */
    public int getShort() {
        return getShort(true, NioByteType.NORMAL, NioByteOrder.BIG);
    }

    /**
     * Reads a standard big-endian {@code short}.
     *
     * @param signed If the short is signed.
     * @return The value of the short.
     */
    public int getShort(boolean signed) {
        return getShort(signed, NioByteType.NORMAL, NioByteOrder.BIG);
    }

    /**
     * Reads a signed big-endian {@code short}.
     *
     * @param type The byte transformation type
     * @return The value of the short.
     */
    public int getShort(NioByteType type) {
        return getShort(true, type, NioByteOrder.BIG);
    }

    /**
     * Reads a big-endian {@code short}.
     *
     * @param signed If the short is signed.
     * @param type The byte transformation type
     * @return The value of the short.
     */
    public int getShort(boolean signed, NioByteType type) {
        return getShort(signed, type, NioByteOrder.BIG);
    }

    /**
     * Reads a signed standard {@code short}.
     *
     * @param order The byte endianness type.
     * @return The value of the short.
     */
    public int getShort(NioByteOrder order) {
        return getShort(true, NioByteType.NORMAL, order);
    }

    /**
     * Reads a standard {@code short}.
     *
     * @param signed If the short is signed.
     * @param order The byte endianness type.
     * @return The value of the short.
     */
    public int getShort(boolean signed, NioByteOrder order) {
        return getShort(signed, NioByteType.NORMAL, order);
    }

    /**
     * Reads a signed {@code short}.
     *
     * @param type The byte transformation type
     * @param order The byte endianness type.
     * @return The value of the short.
     */
    public int getShort(NioByteType type, NioByteOrder order) {
        return getShort(true, type, order);
    }

    /**
     * Reads an {@code int}.
     *
     * @param signed If the integer is signed.
     * @param type The byte transformation type
     * @param order The byte endianness type.
     * @return The value of the integer.
     */
    public int getInt(boolean signed, NioByteType type, NioByteOrder order) {
        long value = 0;
        switch (order) {
            case BIG:
                value |= get(false) << 24;
                value |= get(false) << 16;
                value |= get(false) << 8;
                value |= get(false, type);
                break;
            case MIDDLE:
                value |= get(false) << 8;
                value |= get(false, type);
                value |= get(false) << 24;
                value |= get(false) << 16;
                break;
            case INVERSE_MIDDLE:
                value |= get(false) << 16;
                value |= get(false) << 24;
                value |= get(false, type);
                value |= get(false) << 8;
                break;
            case LITTLE:
                value |= get(false, type);
                value |= get(false) << 8;
                value |= get(false) << 16;
                value |= get(false) << 24;
                break;
        }
        return (int) (signed ? value : value & 0xffffffffL);
    }

    /**
     * Reads a signed standard big-endian {@code int}.
     *
     * @return The value of the integer.
     */
    public int getInt() {
        return getInt(true, NioByteType.NORMAL, NioByteOrder.BIG);
    }

    /**
     * Reads a standard big-endian {@code int}.
     *
     * @param signed If the integer is signed.
     * @return The value of the integer.
     */
    public int getInt(boolean signed) {
        return getInt(signed, NioByteType.NORMAL, NioByteOrder.BIG);
    }

    /**
     * Reads a signed big-endian {@code int}.
     *
     * @param type The byte transformation type
     * @return The value of the integer.
     */
    public int getInt(NioByteType type) {
        return getInt(true, type, NioByteOrder.BIG);
    }

    /**
     * Reads a big-endian {@code int}.
     *
     * @param signed If the integer is signed.
     * @param type The byte transformation type
     * @return The value of the integer.
     */
    public int getInt(boolean signed, NioByteType type) {
        return getInt(signed, type, NioByteOrder.BIG);
    }

    /**
     * Reads a signed standard {@code int}.
     *
     * @param order The byte endianness type.
     * @return The value of the integer.
     */
    public int getInt(NioByteOrder order) {
        return getInt(true, NioByteType.NORMAL, order);
    }

    /**
     * Reads a standard {@code int}.
     *
     * @param signed If the integer is signed.
     * @param order The byte endianness type.
     * @return The value of the integer.
     */
    public int getInt(boolean signed, NioByteOrder order) {
        return getInt(signed, NioByteType.NORMAL, order);
    }

    /**
     * Reads a signed {@code int}.
     *
     * @param type The byte transformation type
     * @param order The byte endianness type.
     * @return The value of the integer.
     */
    public int getInt(NioByteType type, NioByteOrder order) {
        return getInt(true, type, order);
    }

    /**
     * Reads a signed {@code long} value.
     *
     * @param type The byte transformation type
     * @param order The byte endianness type.
     * @return The value of the long.
     * @throws UnsupportedOperationException if middle or inverse-middle value types are selected.
     */
    public long getLong(NioByteType type, NioByteOrder order) {
        long value = 0;
        switch (order) {
            case BIG:
                value |= (long) get(false) << 56L;
                value |= (long) get(false) << 48L;
                value |= (long) get(false) << 40L;
                value |= (long) get(false) << 32L;
                value |= (long) get(false) << 24L;
                value |= (long) get(false) << 16L;
                value |= (long) get(false) << 8L;
                value |= get(false, type);
                break;
            case INVERSE_MIDDLE:
            case MIDDLE:
                throw new UnsupportedOperationException("Middle and inverse-middle value types not supported!");
            case LITTLE:
                value |= get(false, type);
                value |= (long) get(false) << 8L;
                value |= (long) get(false) << 16L;
                value |= (long) get(false) << 24L;
                value |= (long) get(false) << 32L;
                value |= (long) get(false) << 40L;
                value |= (long) get(false) << 48L;
                value |= (long) get(false) << 56L;
                break;
        }
        return value;
    }

    /**
     * Reads a signed standard big-endian {@code long}.
     *
     * @return The value of the long.
     */
    public long getLong() {
        return getLong(NioByteType.NORMAL, NioByteOrder.BIG);
    }

    /**
     * Reads a signed big-endian {@code long}.
     *
     * @param type The byte transformation type.
     * @return The value of the long.
     */
    public long getLong(NioByteType type) {
        return getLong(type, NioByteOrder.BIG);
    }

    /**
     * Reads a signed standard {@code long}.
     *
     * @param order The byte endianness type.
     * @return The value of the long.
     */
    public long getLong(NioByteOrder order) {
        return getLong(NioByteType.NORMAL, order);
    }

    /**
     * Reads a RuneScape {@code String} value.
     *
     * @return The value of the string.
     */
    public String getString() {
        byte temp;
        StringBuilder b = new StringBuilder();
        while ((temp = (byte) get()) != 10) {
            b.append((char) temp);
        }
        return b.toString();
    }

    /**
     * Reads the amount of bytes into the array, starting at the current position.
     *
     * @param amount The amount to read.
     * @return A buffer filled with the data.
     */
    public byte[] getBytes(int amount) {
        return getBytes(amount, NioByteType.NORMAL);
    }

    /**
     * Reads the amount of bytes into a byte array, starting at the current position.
     *
     * @param amount The amount of bytes.
     * @param type The byte transformation type of each byte.
     * @return A buffer filled with the data.
     */
    public byte[] getBytes(int amount, NioByteType type) {
        byte[] data = new byte[amount];
        for (int i = 0; i < amount; i++) {
            data[i] = (byte) get(type);
        }
        return data;
    }

    /**
     * Reads the amount of bytes from the buffer in reverse, starting at {@code current_position + amount} and reading in
     * reverse until the current position.
     *
     * @param amount The amount of bytes to read.
     * @param type The byte transformation type of each byte.
     * @return A buffer filled with the data.
     */
    public byte[] getBytesReverse(int amount, NioByteType type) {
        byte[] data = new byte[amount];
        int dataPosition = 0;
        for (int i = buf.position() + amount - 1; i >= buf.position(); i--) {
            int value = buf.get(i);
            switch (type) {
                case A:
                    value -= 128;
                    break;
                case C:
                    value = -value;
                    break;
                case S:
                    value = 128 - value;
                    break;
                case NORMAL:
                    break;
            }
            data[dataPosition++] = (byte) value;
        }
        return data;
    }

    /**
     * Encodes the public RSA values.
     */
    public void encodeRSA(RsaKeyPair rsaKey) {
        int length = buf.position();
        buf.position(0);

        byte buffer[] = getBytes(length);
        byte rsa[] = buffer;

        if (rsaKey != null) {
            rsa = new BigInteger(buffer).modPow(rsaKey.getExponent(), rsaKey.getModulus()).toByteArray();
        }

        buf.position(0);
        put(rsa.length);
        putBytes(rsa, rsa.length);
    }

    /**
     * @return The backing buffer.
     */
    public ByteBuffer getBuffer() {
        return buf;
    }
}