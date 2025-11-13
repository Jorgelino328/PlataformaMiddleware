package imd.ufrn.br.broker;

import imd.ufrn.br.remoting.Request;
import imd.ufrn.br.remoting.Response;
import imd.ufrn.br.remoting.Invoker;
import imd.ufrn.br.remoting.AsyncInvoker;
import imd.ufrn.br.extensions.ExtensionManager;
import imd.ufrn.br.infra.MetricsCollector;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class Broker {

    private final Invoker invoker;
    private final AsyncInvoker asyncInvoker;
    private final ExtensionManager extensionManager;
    private final MetricsCollector metricsCollector;
    private final boolean asyncMode;
    private final long asyncTimeout;

    public Broker(Invoker invoker, ExtensionManager extensionManager, MetricsCollector metricsCollector) {
        this(invoker, null, extensionManager, metricsCollector, false, 30000);
    }

    public Broker(Invoker invoker, AsyncInvoker asyncInvoker, ExtensionManager extensionManager, 
                  MetricsCollector metricsCollector, boolean asyncMode, long asyncTimeout) {
        if (invoker == null) {
            throw new IllegalArgumentException("Invoker cannot be null.");
        }
        if (asyncMode && asyncInvoker == null) {
            throw new IllegalArgumentException("AsyncInvoker cannot be null when asyncMode is true.");
        }
        this.invoker = invoker;
        this.asyncInvoker = asyncInvoker;
        this.extensionManager = extensionManager;
        this.metricsCollector = metricsCollector;
        this.asyncMode = asyncMode;
        this.asyncTimeout = asyncTimeout;
    }

    public Response invoke(Request request) {
        String serviceName = request.instance().getClass().getSimpleName();
        String methodName = request.method().getName();

        if (extensionManager != null) {
            try {
                extensionManager.notifyInvoke(serviceName, methodName);
            } catch (Exception ignored) {
            }
        }

        long start = System.currentTimeMillis();
        try {
            Object result;
            
            if (asyncMode) {
                CompletableFuture<Object> future = asyncInvoker.invokeAsync(
                    request.instance(), 
                    request.method(), 
                    request.params()
                );
                
                try {
                    result = future.get(asyncTimeout, TimeUnit.MILLISECONDS);
                } catch (TimeoutException e) {
                    return new Response(null, new Exception("Async invocation timeout after " + asyncTimeout + "ms"));
                } catch (ExecutionException e) {
                    return new Response(null, new Exception("Async invocation failed: " + e.getCause().getMessage(), e.getCause()));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return new Response(null, new Exception("Async invocation interrupted", e));
                }
            } else {
                result = invoker.invoke(request.instance(), request.method(), request.params());
            }
            
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
    
    public void shutdown() {
        if (asyncInvoker != null) {
            asyncInvoker.shutdown();
        }
    }
}