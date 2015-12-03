package org.jbot.net.codec;

import java.math.BigInteger;
import java.nio.ByteBuffer;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * The {@link ByteBuffer} wrapper tailored to the specifications of the RuneScape protocol.
 *
 * @author lare96 <http://github.org/lare96>
 */
public final class JBotBuffer {

    /**
     * An array of bit masks used for bitwise operations.
     */
    public static final int[] BIT_MASK = new int[32];

    /**
     * The default capacity of this buffer.
     */
    private static final int DEFAULT_CAP = 128;

    /**
     * The backing byte buffer used to read and write data.
     */
    private ByteBuffer buf;

    /**
     * The position of the buffer when a variable length message is created.
     */
    private int varLengthIndex = 0;

    /**
     * The current bit position when writing bits.
     */
    private int bitIndex = 0;

    /**
     * Creates a new {@link JBotBuffer} with the {@code buf} backing buffer.
     *
     * @param buf The backing buffer used to read and write data.
     */
    private JBotBuffer(ByteBuffer buf) {
        this.buf = buf;
    }

    /**
     * A static initialization block that calculates the bit masks.
     */
    static {
        for (int i = 0; i < BIT_MASK.length; i++) {
            BIT_MASK[i] = (1 << i) - 1;
        }
    }

    /**
     * Creates a new {@link JBotBuffer} with the {@code buf} backing buffer.
     *
     * @param buf The backing buffer used to read and write data.
     * @return The newly created buffer.
     */
    public static JBotBuffer create(ByteBuffer buf) {
        return new JBotBuffer(buf);
    }

    /**
     * Creates a new {@link JBotBuffer} with the {@code cap} as the capacity. The returned buffer will most likely be a
     * direct buffer.
     *
     * @param cap The capacity of the buffer.
     * @return The newly created buffer.
     */
    public static JBotBuffer create(int cap) {
        checkArgument(cap >= 0, "cap < 0");
        return JBotBuffer.create(ByteBuffer.allocate(cap));
    }

    /**
     * Creates a new {@link JBotBuffer} with the default capacity. The returned buffer will most likely be a direct buffer.
     *
     * @return The newly created buffer.
     */
    public static JBotBuffer create() {
        return JBotBuffer.create(DEFAULT_CAP);
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
     * Builds a new message header.
     *
     * @param opcode The opcode of the message.
     * @return An instance of this byte message.
     */
    public JBotBuffer message(int opcode) {
        put(opcode);
        return this;
    }

    /**
     * Builds a new message header for a variable length message. Note that the corresponding {@code endVarMessage()} method
     * must be called to finish the message.
     *
     * @param opcode The opcode of the message.
     * @return An instance of this byte message.
     */
    public JBotBuffer varMessage(int opcode) {
        message(opcode);
        varLengthIndex = buf.position();
        put(0);
        return this;
    }

    /**
     * Builds a new message header for a variable length message, where the length is written as a {@code short} instead of a
     * {@code byte}. Note that the corresponding {@code endVarShortMessage()} method must be called to finish the message.
     *
     * @param opcode The opcode of the message.
     * @return An instance of this byte message.
     */
    public JBotBuffer varShortMessage(int opcode) {
        message(opcode);
        varLengthIndex = buf.position();
        putShort(0);
        return this;
    }

    /**
     * Finishes a variable message header by writing the actual message length at the length {@code byte} position. Call this
     * when the construction of the actual variable length message is complete.
     *
     * @return An instance of this byte message.
     */
    public JBotBuffer endVarMessage() {
        buf.put(varLengthIndex, (byte) (buf.position() - varLengthIndex - 1));
        return this;
    }

    /**
     * Finishes a variable message header by writing the actual message length at the length {@code short} position. Call
     * this when the construction of the actual variable length message is complete.
     *
     * @return An instance of this byte message.
     */
    public JBotBuffer endVarShortMessage() {
        buf.putShort(varLengthIndex, (short) (buf.position() - varLengthIndex - 2));
        return this;
    }

    /**
     * Writes the bytes from the argued buffer into this buffer. This method does not modify the argued buffer, and please do
     * not flip the buffer beforehand.
     *
     * @param from The argued buffer that bytes will be written from.
     * @return An instance of this byte message.
     */
    public JBotBuffer putBytes(ByteBuffer from) {
        for (int i = 0; i < from.position(); i++) {
            put(from.get(i));
        }
        return this;
    }

    /**
     * Writes the bytes from the argued buffer into this buffer.
     *
     * @param from The argued buffer that bytes will be written from.
     * @return An instance of this byte message.
     */
    public JBotBuffer putBytes(byte[] from, int size) {
        buf.put(from, 0, size);
        return this;
    }

    /**
     * Writes the bytes from the argued byte array into this buffer, in reverse.
     *
     * @param data The data to write to this buffer.
     */
    public JBotBuffer putBytesReverse(byte[] data) {
        for (int i = data.length - 1; i >= 0; i--) {
            put(data[i]);
        }
        return this;
    }

    /**
     * Writes the value as a variable amount of bits.
     *
     * @param amount The amount of bits to write.
     * @param value The value of the bits.
     * @return An instance of this byte message.
     * @throws IllegalArgumentException If the number of bits is not between {@code 1} and {@code 32} inclusive.
     */
    public JBotBuffer putBits(int amount, int value) {
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
     * @param flag The flag to write.
     * @return An instance of this byte message.
     */
    public JBotBuffer putBit(boolean flag) {
        putBits(1, flag ? 1 : 0);
        return this;
    }

    /**
     * Writes a value as a {@code byte}.
     *
     * @param value The value to write.
     * @param type The byte transformation type
     * @return An instance of this byte message.
     */
    public JBotBuffer put(int value, JBotByteType type) {
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
     * @return An instance of this byte message.
     */
    public JBotBuffer put(int value) {
        put(value, JBotByteType.NORMAL);
        return this;
    }

    /**
     * Writes a value as a {@code short}.
     *
     * @param value The value to write.
     * @param type The byte transformation type
     * @param order The byte endianness type.
     * @return An instance of this byte message.
     * @throws UnsupportedOperationException If middle or inverse-middle value types are selected.
     */
    public JBotBuffer putShort(int value, JBotByteType type, JBotEndianness order) {
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
     * @return An instance of this byte message.
     */
    public JBotBuffer putShort(int value) {
        putShort(value, JBotByteType.NORMAL, JBotEndianness.BIG);
        return this;
    }

    /**
     * Writes a value as a big-endian {@code short}.
     *
     * @param value The value to write.
     * @param type The byte transformation type
     * @return An instance of this byte message.
     */
    public JBotBuffer putShort(int value, JBotByteType type) {
        putShort(value, type, JBotEndianness.BIG);
        return this;
    }

    /**
     * Writes a value as a standard {@code short}.
     *
     * @param value The value to write.
     * @param order The byte endianness type.
     * @return An instance of this byte message.
     */
    public JBotBuffer putShort(int value, JBotEndianness order) {
        putShort(value, JBotByteType.NORMAL, order);
        return this;
    }

    /**
     * Writes a value as an {@code int}.
     *
     * @param value The value to write.
     * @param type The byte transformation type
     * @param order The byte endianness type.
     * @return An instance of this byte message.
     */
    public JBotBuffer putInt(int value, JBotByteType type, JBotEndianness order) {
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
     * @return An instance of this byte message.
     */
    public JBotBuffer putInt(int value) {
        putInt(value, JBotByteType.NORMAL, JBotEndianness.BIG);
        return this;
    }

    /**
     * Writes a value as a big-endian {@code int}.
     *
     * @param value The value to write.
     * @param type The byte transformation type
     * @return An instance of this byte message.
     */
    public JBotBuffer putInt(int value, JBotByteType type) {
        putInt(value, type, JBotEndianness.BIG);
        return this;
    }

    /**
     * Writes a value as a standard {@code int}.
     *
     * @param value The value to write.
     * @param order The byte endianness type.
     * @return An instance of this byte message.
     */
    public JBotBuffer putInt(int value, JBotEndianness order) {
        putInt(value, JBotByteType.NORMAL, order);
        return this;
    }

    /**
     * Writes a value as a {@code long}.
     *
     * @param value The value to write.
     * @param type The byte transformation type
     * @param order The byte endianness type.
     * @return An instance of this byte message.
     * @throws UnsupportedOperationException If middle or inverse-middle value types are selected.
     */
    public JBotBuffer putLong(long value, JBotByteType type, JBotEndianness order) {
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
     * @return An instance of this byte message.
     */
    public JBotBuffer putLong(long value) {
        putLong(value, JBotByteType.NORMAL, JBotEndianness.BIG);
        return this;
    }

    /**
     * Writes a value as a big-endian {@code long}.
     *
     * @param value The value to write.
     * @param type The byte transformation type
     * @return An instance of this byte message.
     */
    public JBotBuffer putLong(long value, JBotByteType type) {
        putLong(value, type, JBotEndianness.BIG);
        return this;
    }

    /**
     * Writes a value as a standard {@code long}.
     *
     * @param value The value to write.
     * @param order The byte endianness type. to write.
     * @return An instance of this byte message.
     */
    public JBotBuffer putLong(long value, JBotEndianness order) {
        putLong(value, JBotByteType.NORMAL, order);
        return this;
    }

    /**
     * Writes a RuneScape {@code String} value.
     *
     * @param string The string to write.
     * @return An instance of this byte message.
     */
    public JBotBuffer putString(String string) {
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
    public int get(boolean signed, JBotByteType type) {
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
        return get(true, JBotByteType.NORMAL);
    }

    /**
     * Reads a standard {@code byte}.
     *
     * @param signed If the byte is signed.
     * @return The value of the byte.
     */
    public int get(boolean signed) {
        return get(signed, JBotByteType.NORMAL);
    }

    /**
     * Reads a signed {@code byte}.
     *
     * @param type The byte transformation type
     * @return The value of the byte.
     */
    public int get(JBotByteType type) {
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
    public int getShort(boolean signed, JBotByteType type, JBotEndianness order) {
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
        return getShort(true, JBotByteType.NORMAL, JBotEndianness.BIG);
    }

    /**
     * Reads a standard big-endian {@code short}.
     *
     * @param signed If the short is signed.
     * @return The value of the short.
     */
    public int getShort(boolean signed) {
        return getShort(signed, JBotByteType.NORMAL, JBotEndianness.BIG);
    }

    /**
     * Reads a signed big-endian {@code short}.
     *
     * @param type The byte transformation type
     * @return The value of the short.
     */
    public int getShort(JBotByteType type) {
        return getShort(true, type, JBotEndianness.BIG);
    }

    /**
     * Reads a big-endian {@code short}.
     *
     * @param signed If the short is signed.
     * @param type The byte transformation type
     * @return The value of the short.
     */
    public int getShort(boolean signed, JBotByteType type) {
        return getShort(signed, type, JBotEndianness.BIG);
    }

    /**
     * Reads a signed standard {@code short}.
     *
     * @param order The byte endianness type.
     * @return The value of the short.
     */
    public int getShort(JBotEndianness order) {
        return getShort(true, JBotByteType.NORMAL, order);
    }

    /**
     * Reads a standard {@code short}.
     *
     * @param signed If the short is signed.
     * @param order The byte endianness type.
     * @return The value of the short.
     */
    public int getShort(boolean signed, JBotEndianness order) {
        return getShort(signed, JBotByteType.NORMAL, order);
    }

    /**
     * Reads a signed {@code short}.
     *
     * @param type The byte transformation type
     * @param order The byte endianness type.
     * @return The value of the short.
     */
    public int getShort(JBotByteType type, JBotEndianness order) {
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
    public int getInt(boolean signed, JBotByteType type, JBotEndianness order) {
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
        return getInt(true, JBotByteType.NORMAL, JBotEndianness.BIG);
    }

    /**
     * Reads a standard big-endian {@code int}.
     *
     * @param signed If the integer is signed.
     * @return The value of the integer.
     */
    public int getInt(boolean signed) {
        return getInt(signed, JBotByteType.NORMAL, JBotEndianness.BIG);
    }

    /**
     * Reads a signed big-endian {@code int}.
     *
     * @param type The byte transformation type
     * @return The value of the integer.
     */
    public int getInt(JBotByteType type) {
        return getInt(true, type, JBotEndianness.BIG);
    }

    /**
     * Reads a big-endian {@code int}.
     *
     * @param signed If the integer is signed.
     * @param type The byte transformation type
     * @return The value of the integer.
     */
    public int getInt(boolean signed, JBotByteType type) {
        return getInt(signed, type, JBotEndianness.BIG);
    }

    /**
     * Reads a signed standard {@code int}.
     *
     * @param order The byte endianness type.
     * @return The value of the integer.
     */
    public int getInt(JBotEndianness order) {
        return getInt(true, JBotByteType.NORMAL, order);
    }

    /**
     * Reads a standard {@code int}.
     *
     * @param signed If the integer is signed.
     * @param order The byte endianness type.
     * @return The value of the integer.
     */
    public int getInt(boolean signed, JBotEndianness order) {
        return getInt(signed, JBotByteType.NORMAL, order);
    }

    /**
     * Reads a signed {@code int}.
     *
     * @param type The byte transformation type
     * @param order The byte endianness type.
     * @return The value of the integer.
     */
    public int getInt(JBotByteType type, JBotEndianness order) {
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
    public long getLong(JBotByteType type, JBotEndianness order) {
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
        return getLong(JBotByteType.NORMAL, JBotEndianness.BIG);
    }

    /**
     * Reads a signed big-endian {@code long}.
     *
     * @param type The byte transformation type.
     * @return The value of the long.
     */
    public long getLong(JBotByteType type) {
        return getLong(type, JBotEndianness.BIG);
    }

    /**
     * Reads a signed standard {@code long}.
     *
     * @param order The byte endianness type.
     * @return The value of the long.
     */
    public long getLong(JBotEndianness order) {
        return getLong(JBotByteType.NORMAL, order);
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
        return getBytes(amount, JBotByteType.NORMAL);
    }

    /**
     * Reads the amount of bytes into a byte array, starting at the current position.
     *
     * @param amount The amount of bytes.
     * @param type The byte transformation type of each byte.
     * @return A buffer filled with the data.
     */
    public byte[] getBytes(int amount, JBotByteType type) {
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
    public byte[] getBytesReverse(int amount, JBotByteType type) {
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
        //36

    }

    /**
     * Encodes RSA into this {@code ByteMessage}.
     *
     * @param rsaKey The public RSA key.
     */
    public void encodeRSA(JBotRsaKey rsaKey) {
        int length = buf.position();
        buf.position(0);

        byte rsa[] = getBytes(length);

        if (rsaKey != null) {
            rsa = new BigInteger(buf.array()).modPow(rsaKey.getExponent(), rsaKey.getModulus()).toByteArray();
        }

        buf.position(0);
        put(rsa.length);
        putBytes(rsa, rsa.length);
    }

    /**
     * Gets the backing byte buffer used to read and write data.
     *
     * @return The backing byte buffer.
     */
    public ByteBuffer getBuffer() {
        return buf;
    }
}