package io.rsbot.net;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Spliterator;

/**
 * A linked list used for storing decoded networking Objects.
 *
 * @author lare96 <http://github.com/lare96>
 */
public final class MessageList implements Iterable<Object> {

    /**
     * A list of decoded messages.
     */
    private final List<Object> messages = new LinkedList<>();

    /**
     * The returned iterator is <strong>immutable</strong>.
     */
    @Override
    public Iterator<Object> iterator() {
        return Collections.unmodifiableList(messages).iterator();

    }

    /**
     * Returns a mutable iterator.
     */
    Iterator<Object> mutableIterator() {
        return messages.iterator();
    }

    @Override
    public Spliterator<Object> spliterator() {
        return messages.spliterator();
    }

    /**
     * Adds a decoded message.
     */
    public void add(Object msg) {
        messages.add(msg);
    }

    /**
     * Creates a new message list.
     */
    MessageList() {
    }
}
