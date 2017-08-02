package io.rsbot.net.codec;

import java.math.BigInteger;

/**
 * A model representing an RSA modulus and exponent pair.
 *
 * @author lare96 <http://github.com/lare96>
 */
public final class RsaKeyPair {

    /**
     * The modulus value.
     */
    private final BigInteger modulus;

    /**
     * The exponent value.
     */
    private final BigInteger exponent;

    /**
     * Creates a new {@link RsaKeyPair}.
     *
     * @param modulus The modulus value.
     * @param exponent The exponent value.
     */
    public RsaKeyPair(BigInteger modulus, BigInteger exponent) {
        this.modulus = modulus;
        this.exponent = exponent;
    }

    /**
     * @return The modulus value.
     */
    public BigInteger getModulus() {
        return modulus;
    }

    /**
     * @return The exponent value.
     */
    public BigInteger getExponent() {
        return exponent;
    }
}
