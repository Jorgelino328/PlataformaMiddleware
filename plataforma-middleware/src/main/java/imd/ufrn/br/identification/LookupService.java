package imd.ufrn.br.identification; // Your package structure

import imd.ufrn.br.annotations.RemoteObject;
import imd.ufrn.br.exceptions.ObjectNotFoundException;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The LookupService is responsible for mapping {@link ObjectId}s to actual
 * remote object instances. It allows for the registration and retrieval
 * of remote objects.
 *
 * This implementation uses a simple in-memory map and is designed as a singleton.
 */
public class LookupService {

    private static final LookupService INSTANCE = new LookupService();


    private final Map<ObjectId, Object> remoteObjectsRegistry;

    /**
     * Private constructor to enforce singleton pattern.
     * Initializes the remote objects' registry.
     */
    private LookupService() {
        this.remoteObjectsRegistry = new ConcurrentHashMap<>();
    }

    /**
     * Returns the singleton instance of the LookupService.
     *
     * @return The singleton LookupService instance.
     */
    public static LookupService getInstance() {
        return INSTANCE;
    }

    /**
     * Registers a remote object instance with the given ObjectId.
     * The object's class must be annotated with {@link RemoteObject}.
     * If an object with the same ObjectId is already registered, it will be overwritten.
     *
     * @param id The {@link ObjectId} to associate with the object. Must not be null.
     * @param objectInstance The actual remote object instance. Must not be null.
     * @throws IllegalArgumentException if id or objectInstance is null, or if the
     *                                  objectInstance's class is not annotated with {@link RemoteObject}.
     */
    public void registerObject(ObjectId id, Object objectInstance) {
        if (id == null) {
            throw new IllegalArgumentException("ObjectId cannot be null for registration.");
        }
        if (objectInstance == null) {
            throw new IllegalArgumentException("Object instance cannot be null for registration.");
        }

        if (!objectInstance.getClass().isAnnotationPresent(RemoteObject.class)) {
            throw new IllegalArgumentException("Object of type " + objectInstance.getClass().getName() +
                    " is not annotated with @RemoteObject and cannot be registered.");
        }

        remoteObjectsRegistry.put(id, objectInstance);
        System.out.println("LookupService: Registered object '" + id.getId() +
                "' -> " + objectInstance.getClass().getName());
    }

    /**
     * Finds and returns the remote object instance associated with the given ObjectId.
     *
     * @param id The {@link ObjectId} of the remote object to find. Must not be null.
     * @return The remote object instance.
     * @throws IllegalArgumentException if id is null.
     * @throws ObjectNotFoundException if no object is registered with the given ObjectId.
     */
    public Object findObject(ObjectId id) throws ObjectNotFoundException {
        if (id == null) {
            throw new IllegalArgumentException("ObjectId cannot be null for lookup.");
        }

        Object objectInstance = remoteObjectsRegistry.get(id);

        if (objectInstance == null) {
            throw new ObjectNotFoundException("No object found registered with ObjectId: " + id.getId());
        }

        System.out.println("LookupService: Found object '" + id.getId() +
                "' -> " + objectInstance.getClass().getName());
        return objectInstance;
    }

    /**
     * Removes a remote object instance from the registry.
     *
     * @param id The {@link ObjectId} of the object to unregister.
     * @return The object instance that was removed, or null if no object was
     *         registered with that id.
     */
    public Object unregisterObject(ObjectId id) {
        if (id == null) {
            System.err.println("LookupService: Attempted to unregister with a null ObjectId.");
            return null;
        }
        Object removedObject = remoteObjectsRegistry.remove(id);
        if (removedObject != null) {
            System.out.println("LookupService: Unregistered object '" + id.getId() + "'");
        } else {
            System.out.println("LookupService: No object found with id '" + id.getId() + "' to unregister.");
        }
        return removedObject;
    }

    /**
     * Clears all registered objects from the service.
     * Useful for testing or re-initialization.
     */
    public void clearRegistry() {
        remoteObjectsRegistry.clear();
        System.out.println("LookupService: Registry cleared.");
    }

    /**
     * Gets the current number of registered objects.
     * @return the count of registered objects.
     */
    public int getRegisteredObjectCount() {
        return remoteObjectsRegistry.size();
    }
}