package imd.ufrn.br.broker;

import imd.ufrn.br.identification.ObjectId;
import imd.ufrn.br.remoting.Invoker;

public class Broker {

    private final Invoker invoker;

    public Broker(Invoker invoker) {
        if (invoker == null) {
            throw new IllegalArgumentException("Invoker cannot be null.");
        }
        this.invoker = invoker;
    }

    public Object processRequest(ObjectId objectId, String methodName, Object[] params) throws Throwable {
        System.out.println("Broker: Processing request for ObjectId: " + (objectId != null ? objectId.getId() : "null") +
                ", Method: " + methodName);

        return invoker.invoke(objectId, methodName, params);
    }
}