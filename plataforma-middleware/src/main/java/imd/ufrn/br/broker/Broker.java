package imd.ufrn.br.broker;

import imd.ufrn.br.remoting.Request;
import imd.ufrn.br.remoting.Response;
import imd.ufrn.br.remoting.Invoker;
import imd.ufrn.br.extensions.ExtensionManager;
import imd.ufrn.br.infra.MetricsCollector;

public class Broker {

    private final Invoker invoker;
    private final ExtensionManager extensionManager;
    private final MetricsCollector metricsCollector;

    public Broker(Invoker invoker, ExtensionManager extensionManager, MetricsCollector metricsCollector) {
        if (invoker == null) {
            throw new IllegalArgumentException("Invoker cannot be null.");
        }
        this.invoker = invoker;
        this.extensionManager = extensionManager;
        this.metricsCollector = metricsCollector;
    }

    public Response invoke(Request request) {
        String serviceName = request.instance().getClass().getSimpleName();
        String methodName = request.method().getName();

        System.out.println("Broker: Processing direct request for " + serviceName + "." + methodName);

        if (extensionManager != null) {
            try {
                extensionManager.notifyInvoke(serviceName, methodName);
            } catch (Exception ignored) {
            }
        }

        long start = System.currentTimeMillis();
        try {
            // Direct invocation, no need for async logic here as HTTP is request-response
            Object result = invoker.invoke(request.instance(), request.method(), request.params());
            return new Response(result, null);
        } catch (Throwable e) {
            return new Response(null, new Exception(e));
        } finally {
            long latency = System.currentTimeMillis() - start;
            if (metricsCollector != null) {
                try {
                    metricsCollector.record(serviceName, methodName, latency);
                } catch (Exception ignored) {
                }
            }
        }
    }
}