package imd.ufrn.br.broker;

import imd.ufrn.br.identification.ObjectId;
import imd.ufrn.br.remoting.Invoker;

/**
 * The Broker acts as an intermediary, decoupling the component that receives
 * requests (e.g., ServerRequestHandler) from the component that executes
 * method invocations on remote objects (Invoker).
 *
 * It receives details of a remote invocation request (object ID, method name,
 * parameters) and delegates the actual invocation to an {@link Invoker}.
 */
public class Broker {

    private final Invoker invoker;

    /**
     * Constructs a new Broker with the specified Invoker.
     * The Broker will use this Invoker to perform method executions.
     *
     * @param invoker The {@link Invoker} instance to be used for method invocations.
     *                Cannot be null.
     */
    public Broker(Invoker invoker) {
        if (invoker == null) {
            throw new IllegalArgumentException("Invoker cannot be null.");
        }
        this.invoker = invoker;
    }

    /**
     * Processes a remote method invocation request.
     * This method is typically called by a {@code ServerRequestHandler} after
     * it has received and unmarshalled an incoming request.
     *
     * The Broker delegates the actual method execution to its configured {@link Invoker}.
     *
     * @param objectId The {@link ObjectId} identifying the target remote object.
     * @param methodName The name of the method to be invoked on the remote object.
     * @param params An array of objects representing the parameters for the method invocation.
     *               These parameters should already be unmarshalled into their Java types.
     * @return The result of the method invocation.
     * @throws Throwable If any error occurs during the invocation, including
     *                   {@code ObjectNotFoundException}, {@code NoSuchMethodException},
     *                   or exceptions thrown by the invoked method itself.
     */
    public Object processRequest(ObjectId objectId, String methodName, Object[] params) throws Throwable {
        System.out.println("Broker: Processing request for ObjectId: " + (objectId != null ? objectId.getId() : "null") +
                ", Method: " + methodName);

        return invoker.invoke(objectId, methodName, params);
    }
}