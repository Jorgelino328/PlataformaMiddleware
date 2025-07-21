package imd.ufrn.br.identification;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public final class ObjectId implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String id;

    public ObjectId(String id) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("Object ID string cannot be null or empty.");
        }
        this.id = id;
    }

    public String getId() {
        return id;
    }

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