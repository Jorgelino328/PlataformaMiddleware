package imd.ufrn.br.lifecycle;

import imd.ufrn.br.identification.ObjectId;
import imd.ufrn.br.identification.LookupService;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Lightweight lifecycle manager that will call lifecycle methods on components
 * that implement the Lifecycle interface. Components are discovered by ObjectId when registered.
 */
public class LifecycleManager {

    private final Map<ObjectId, Lifecycle> managed = new ConcurrentHashMap<>();
    private final LookupService lookupService;

    public LifecycleManager(LookupService lookupService) {
        this.lookupService = lookupService;
    }

    public void register(ObjectId id) {
        try {
            Object obj = lookupService.findObject(id);
            if (obj instanceof Lifecycle) {
                managed.put(id, (Lifecycle) obj);
                ((Lifecycle) obj).init();
                System.out.println("LifecycleManager: Initialized component " + id.getId());
            }
        } catch (Exception e) {
            // Ignore non-managed objects
        }
    }

    public void startAll() {
        managed.forEach((id, comp) -> {
            comp.start();
            System.out.println("LifecycleManager: Started component " + id.getId());
        });
    }

    public void stopAll() {
        managed.forEach((id, comp) -> {
            try {
                comp.stop();
                System.out.println("LifecycleManager: Stopped component " + id.getId());
            } catch (Exception e) {
                System.err.println("LifecycleManager: Error stopping component " + id.getId() + " - " + e.getMessage());
            }
        });
    }
}
