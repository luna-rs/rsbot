package io.rsbot.net.codec;

/**
 * A model representing an ISAAC encryption/decryption pair.
 *
 * @author lare96 <http://github.com/lare96>
 */
public final class IsaacPair {

    /**
     * The encryptor.
     */
    private final Isaac encryptor;

    /**
     * The decryptor.
     */
    private final Isaac decryptor;

    /**
     * Creates a new {@link IsaacPair}.
     *
     * @param encryptor The encryptor.
     * @param decryptor The decryptor.
     */
    public IsaacPair(Isaac encryptor, Isaac decryptor) {
        this.encryptor = encryptor;
        this.decryptor = decryptor;
    }

    /**
     * @return The encryptor.
     */
    public Isaac getEncryptor() {
        return encryptor;
    }

    /**
     * @return The decryptor.
     */
    public Isaac getDecryptor() {
        return decryptor;
    }
}
