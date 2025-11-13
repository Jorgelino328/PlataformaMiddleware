package imd.ufrn.br.remoting;

import java.lang.reflect.Method;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AsyncInvoker {

    private final Invoker invoker;
    private final ExecutorService executor;

    public AsyncInvoker(int poolSize) {
        this.invoker = new Invoker();
        this.executor = Executors.newFixedThreadPool(Math.max(1, poolSize));
    }

    public CompletableFuture<Object> invokeAsync(Object targetObject, Method method, Object[] args) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return invoker.invoke(targetObject, method, args);
            } catch (Throwable t) {
                throw new RuntimeException(t);
            }
        }, executor);
    }

    public void shutdown() {
        executor.shutdown();
    }
}
