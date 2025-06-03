package imd.ufrn.br.identification;

import java.util.Objects;

/**
 * Represents an Absolute Object Reference (AOR).
 * An AOR contains all the information necessary to uniquely identify and locate
 * a specific remote object instance.
 */
public class AbsoluteObjectReference {

    private final ObjectId objectId;
    private final String networkAddress; // Added networkAddress field

    /**
     * Constructs an AbsoluteObjectReference with the given ObjectId and networkAddress.
     *
     * @param objectId      The {@link ObjectId} that uniquely identifies the remote object
     *                      within its server context. Must not be null.
     * @param networkAddress The network address (e.g., host:port) of the server handling the object.
     *                      Must not be null or empty.
     */
    public AbsoluteObjectReference(ObjectId objectId, String networkAddress) {
        if (objectId == null) {
            throw new IllegalArgumentException("ObjectId cannot be null for an AbsoluteObjectReference.");
        }
        if (networkAddress == null || networkAddress.isEmpty()) {
            throw new IllegalArgumentException("NetworkAddress cannot be null or empty for an AbsoluteObjectReference.");
        }
        this.objectId = objectId;
        this.networkAddress = networkAddress;
    }

    /**
     * Gets the ObjectId component of this AOR.
     *
     * @return The {@link ObjectId}.
     */
    public ObjectId getObjectId() {
        return objectId;
    }

    /**
     * Gets the network address component of this AOR.
     *
     * @return The network address.
     */
    public String getNetworkAddress() {
        return networkAddress;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AbsoluteObjectReference that = (AbsoluteObjectReference) o;
        return Objects.equals(objectId, that.objectId) &&
                Objects.equals(networkAddress, that.networkAddress);
    }

    @Override
    public int hashCode() {
        return Objects.hash(objectId, networkAddress);
    }

    @Override
    public String toString() {
        return "AOR[" + objectId + "@" + networkAddress + "]";
    }
}