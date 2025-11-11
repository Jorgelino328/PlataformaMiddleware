package imd.ufrn.br.identification;

import imd.ufrn.br.annotations.RequestMapping;
import imd.ufrn.br.exceptions.ObjectNotFoundException;
import imd.ufrn.br.extensions.ExtensionManager;
import imd.ufrn.br.lifecycle.LifecycleManager;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class LookupService {

    private static final LookupService INSTANCE = new LookupService();

    private final Map<ObjectId, Object> remoteObjectsRegistry;
    private ExtensionManager extensionManager;
    private LifecycleManager lifecycleManager;

    private LookupService() {
        this.remoteObjectsRegistry = new ConcurrentHashMap<>();
    }

    public static LookupService getInstance() {
        return INSTANCE;
    }

    public void registerObject(ObjectId id, Object objectInstance) {
        if (id == null) {
            throw new IllegalArgumentException("ObjectId cannot be null for registration.");
        }
        if (objectInstance == null) {
            throw new IllegalArgumentException("Object instance cannot be null for registration.");
        }

        if (!objectInstance.getClass().isAnnotationPresent(RequestMapping.class)) {
            throw new IllegalArgumentException("Object of type " + objectInstance.getClass().getName() +
                    " is not annotated with @RequestMapping and cannot be registered.");
        }

        remoteObjectsRegistry.put(id, objectInstance);
        System.out.println("LookupService: Registered object '" + id.getId() +
                "' -> " + objectInstance.getClass().getName());
        if (extensionManager != null) {
            extensionManager.notifyRegister(id.getId());
        }
        if (lifecycleManager != null) {
            lifecycleManager.register(id);
        }
    }

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

    public Object unregisterObject(ObjectId id) {
        if (id == null) {
            System.err.println("LookupService: Attempted to unregister with a null ObjectId.");
            return null;
        }
        Object removedObject = remoteObjectsRegistry.remove(id);
        if (removedObject != null) {
            System.out.println("LookupService: Unregistered object '" + id.getId() + "'");
            if (extensionManager != null) {
                extensionManager.notifyUnregister(id.getId());
            }
        } else {
            System.out.println("LookupService: No object found with id '" + id.getId() + "' to unregister.");
        }
        return removedObject;
    }

    public void setExtensionManager(ExtensionManager extensionManager) {
        this.extensionManager = extensionManager;
    }

    public void setLifecycleManager(LifecycleManager lifecycleManager) {
        this.lifecycleManager = lifecycleManager;
    }

    public void clearRegistry() {
        remoteObjectsRegistry.clear();
        System.out.println("LookupService: Registry cleared.");
    }

    public int getRegisteredObjectCount() {
        return remoteObjectsRegistry.size();
    }
}