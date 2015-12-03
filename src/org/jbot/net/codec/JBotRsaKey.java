package org.jbot.net.codec;

import java.math.BigInteger;

/**
 * An immutable class that holds data representing a single RSA key within an RSA key-pair. This is used for security when
 * encoding the credentials of the login protocol.
 *
 * @author lare96 <http://github.org/lare96>
 */
public final class JBotRsaKey {

    /**
     * The final modulus RSA value.
     */
    private final BigInteger modulus;

    /**
     * The final exponent RSA value.
     */
    private final BigInteger exponent;

    /**
     * Creates a new {@link JBotRsaKey}.
     *
     * @param modulus The final modulus RSA value.
     * @param exponent The final exponent RSA value.
     */
    public JBotRsaKey(BigInteger modulus, BigInteger exponent) {
        this.modulus = modulus;
        this.exponent = exponent;
    }

    /**
     * @return The final modulus RSA value.
     */
    public BigInteger getModulus() {
        return modulus;
    }

    /**
     * @return The final exponent RSA value.
     */
    public BigInteger getExponent() {
        return exponent;
    }
}
