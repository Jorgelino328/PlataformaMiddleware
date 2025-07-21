package imd.ufrn.br.identification;

import java.util.Objects;

public class AbsoluteObjectReference {

    private final ObjectId objectId;
    private final String networkAddress;

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

    public ObjectId getObjectId() {
        return objectId;
    }

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