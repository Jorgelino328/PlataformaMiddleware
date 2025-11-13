package imd.ufrn.br.servidor.middleware.management;

import java.util.ArrayList;
import java.util.List;

public class LifecycleManager {

    private final List<Object> managedComponents = new ArrayList<>();

    public void registerComponent(Object component) {
        managedComponents.add(component);
        System.out.println("Component registered in LifecycleManager: " + component.getClass().getSimpleName());
    }

    public void startAll() {
        System.out.println("Starting all managed components...");
        // In a real scenario, you would call a 'start()' method on each component
    }

    public void stopAll() {
        System.out.println("Stopping all managed components...");
        // In a real scenario, you would call a 'stop()' method on each component
    }
}
