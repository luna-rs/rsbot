package io.rsbot.net.codec;

/**
 * An enumerated type describing RuneScape value types.
 *
 * @author lare96 <http://github.com/lare96>
 */
public enum NioByteType {

    /**
     * Do nothing to the value.
     */
    NORMAL,

    /**
     * Add {@code 128} to the value.
     */
    A,

    /**
     * Invert the sign of the value.
     */
    C,

    /**
     * Subtract {@code 128} from the value.
     */
    S
}