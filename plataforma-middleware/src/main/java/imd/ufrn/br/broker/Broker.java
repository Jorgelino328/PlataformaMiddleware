package imd.ufrn.br.broker;

import imd.ufrn.br.identification.ObjectId;
import imd.ufrn.br.remoting.Invoker;
import imd.ufrn.br.remoting.AsyncInvoker;
import imd.ufrn.br.extensions.ExtensionManager;
import imd.ufrn.br.infra.MetricsCollector;

import java.util.concurrent.CompletableFuture;

public class Broker {

    private final Invoker invoker;
    private final AsyncInvoker asyncInvoker;
    private final ExtensionManager extensionManager;
    private final MetricsCollector metricsCollector;

    public Broker(Invoker invoker) {
        if (invoker == null) {
            throw new IllegalArgumentException("Invoker cannot be null.");
        }
        this.invoker = invoker;
        this.asyncInvoker = null;
        this.extensionManager = null;
        this.metricsCollector = null;
    }

    public Broker(Invoker invoker, AsyncInvoker asyncInvoker, ExtensionManager extensionManager) {
        if (invoker == null) {
            throw new IllegalArgumentException("Invoker cannot be null.");
        }
        this.invoker = invoker;
        this.asyncInvoker = asyncInvoker;
        this.extensionManager = extensionManager;
        this.metricsCollector = null;
    }

    public Broker(Invoker invoker, AsyncInvoker asyncInvoker, ExtensionManager extensionManager, MetricsCollector metricsCollector) {
        if (invoker == null) {
            throw new IllegalArgumentException("Invoker cannot be null.");
        }
        this.invoker = invoker;
        this.asyncInvoker = asyncInvoker;
        this.extensionManager = extensionManager;
        this.metricsCollector = metricsCollector;
    }

    public Object processRequest(ObjectId objectId, String methodName, Object[] params) throws Throwable {
        System.out.println("Broker: Processing request for ObjectId: " + (objectId != null ? objectId.getId() : "null") +
                ", Method: " + methodName);

        if (extensionManager != null) {
            try { extensionManager.notifyInvoke(objectId.getId(), methodName); } catch (Exception ex) { }
        }

        long start = System.currentTimeMillis();
        try {
            if (asyncInvoker != null) {
                CompletableFuture<Object> future = asyncInvoker.invokeAsync(objectId, methodName, params);
                Object res = future.get();
                return res;
            }

            return invoker.invoke(objectId, methodName, params);
        } finally {
            long latency = System.currentTimeMillis() - start;
            if (metricsCollector != null) {
                try { metricsCollector.record(objectId.getId(), methodName, latency); } catch (Exception ex) { }
            }
        }
    }
}