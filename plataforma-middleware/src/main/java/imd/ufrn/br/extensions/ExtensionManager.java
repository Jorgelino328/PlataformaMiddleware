package imd.ufrn.br.extensions;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Runtime manager for extensions. For simplicity we accept programmatic registration.
 */
public class ExtensionManager {

    private final List<Extension> extensions = new CopyOnWriteArrayList<>();

    public void registerExtension(Extension extension) {
        if (extension != null) {
            extensions.add(extension);
            System.out.println("ExtensionManager: Registered extension " + extension.getClass().getName());
        }
    }

    public void unregisterExtension(Extension extension) {
        extensions.remove(extension);
    }

    public void notifyRegister(String objectId) {
        for (Extension e : extensions) {
            try { e.onRegister(objectId); } catch (Exception ex) { System.err.println("Extension error: " + ex.getMessage()); }
        }
    }

    public void notifyUnregister(String objectId) {
        for (Extension e : extensions) {
            try { e.onUnregister(objectId); } catch (Exception ex) { System.err.println("Extension error: " + ex.getMessage()); }
        }
    }

    public void notifyInvoke(String objectId, String methodName) {
        for (Extension e : extensions) {
            try { e.onInvoke(objectId, methodName); } catch (Exception ex) { System.err.println("Extension error: " + ex.getMessage()); }
        }
    }
}
