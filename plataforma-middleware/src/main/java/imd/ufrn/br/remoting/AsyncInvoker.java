package imd.ufrn.br.remoting;

import imd.ufrn.br.identification.LookupService;
import imd.ufrn.br.identification.ObjectId;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Provides asynchronous invocation semantics backed by a configurable thread pool.
 */
public class AsyncInvoker {

    private final Invoker invoker;
    private final ExecutorService executor;

    public AsyncInvoker(LookupService lookupService, int poolSize) {
        this.invoker = new Invoker(lookupService);
        this.executor = Executors.newFixedThreadPool(Math.max(1, poolSize));
        System.out.println("AsyncInvoker: Created with " + poolSize + " threads");
    }

    public CompletableFuture<Object> invokeAsync(ObjectId objectId, String methodName, Object[] args) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return invoker.invoke(objectId, methodName, args);
            } catch (Throwable t) {
                throw new RuntimeException(t);
            }
        }, executor);
    }

    public void shutdown() {
        executor.shutdown();
    }
}
