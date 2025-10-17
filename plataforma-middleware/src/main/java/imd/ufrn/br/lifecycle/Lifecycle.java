package imd.ufrn.br.lifecycle;

/**
 * Simple lifecycle contract for remote components.
 */
public interface Lifecycle {
    /**
     * Initialize resources required by the component.
     */
    void init();

    /**
     * Start the component (make it available to serve requests).
     */
    void start();

    /**
     * Stop the component and release resources.
     */
    void stop();
}
