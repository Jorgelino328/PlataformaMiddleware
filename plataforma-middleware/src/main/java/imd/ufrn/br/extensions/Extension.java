package imd.ufrn.br.extensions;

/**
 * Extension SPI for the middleware. Implementations can hook into lifecycle events.
 */
public interface Extension {
    default void onRegister(String objectId) {}
    default void onUnregister(String objectId) {}
    default void onInvoke(String objectId, String methodName) {}
}
