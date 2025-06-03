package imd.ufrn.br.identification;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/**
 * Represents a unique identifier for a remote object instance within its
 * context (e.g., within a server process).
 *
 * This implementation uses a String to hold the ID. It's important that
 * instances of ObjectId correctly implement equals() and hashCode()
 * if they are used as keys in Maps or stored in Sets.
 */
public final class ObjectId implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String id;

    /**
     * Constructs an ObjectId with the given string identifier.
     *
     * @param id The string representation of the object ID. Must not be null or empty.
     * @throws IllegalArgumentException if the id is null or empty.
     */
    public ObjectId(String id) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("Object ID string cannot be null or empty.");
        }
        this.id = id;
    }

    /**
     * Gets the string representation of this object ID.
     *
     * @return The ID string.
     */
    public String getId() {
        return id;
    }

    /**
     * (Alternative Constructor Example)
     * Creates a new ObjectId with a randomly generated UUID as its ID.
     * This can be useful if you need to ensure global uniqueness without
     * relying on user-provided names.
     *
     * @return A new ObjectId instance with a UUID.
     */
    public static ObjectId generate() {
        return new ObjectId(UUID.randomUUID().toString());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ObjectId objectId = (ObjectId) o;
        return id.equals(objectId.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "ObjectId[" + id + "]";
    }
}