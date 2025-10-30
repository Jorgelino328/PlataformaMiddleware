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
    
    public AbsoluteObjectReference(ObjectId objectId, String host, int port) {
        this(objectId, host + ":" + port);
    }

    public ObjectId getObjectId() {
        return objectId;
    }

    public String getNetworkAddress() {
        return networkAddress;
    }
    
    public String getHost() {
        String[] parts = networkAddress.split(":");
        return parts.length > 0 ? parts[0] : networkAddress;
    }
    
    public int getPort() {
        String[] parts = networkAddress.split(":");
        if (parts.length >= 2) {
            try {
                return Integer.parseInt(parts[1]);
            } catch (NumberFormatException e) {
                return -1;
            }
        }
        return -1;
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