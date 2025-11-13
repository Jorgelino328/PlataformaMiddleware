package imd.ufrn.br.lifecycle;

import java.util.ArrayList;
import java.util.List;

public class LifecycleManager {
    
    private final List<Lifecycle> components = new ArrayList<>();
    
    public void register(Lifecycle component) {
        if (component != null && !components.contains(component)) {
            components.add(component);
        }
    }
    
    public void unregister(Lifecycle component) {
        components.remove(component);
    }
    
    public void startAll() {
        for (Lifecycle component : components) {
            try {
                if (!component.isRunning()) {
                    component.start();
                }
            } catch (Exception e) {
                System.err.println("LifecycleManager: Error starting " + component.getClass().getSimpleName() + ": " + e.getMessage());
            }
        }
    }
    
    public void stopAll() {
        for (int i = components.size() - 1; i >= 0; i--) {
            Lifecycle component = components.get(i);
            try {
                if (component.isRunning()) {
                    component.stop();
                }
            } catch (Exception e) {
                System.err.println("LifecycleManager: Error stopping " + component.getClass().getSimpleName() + ": " + e.getMessage());
            }
        }
    }
    
    public void restartAll() {
        stopAll();
        startAll();
    }
    
    public int getComponentCount() {
        return components.size();
    }
    
    public boolean areAllRunning() {
        for (Lifecycle component : components) {
            if (!component.isRunning()) {
                return false;
            }
        }
        return !components.isEmpty();
    }
}
