package imd.ufrn.br;

import imd.ufrn.br.annotations.RequestMapping;
import imd.ufrn.br.broker.Broker;
import imd.ufrn.br.config.MiddlewareConfig;
import imd.ufrn.br.extensions.Extension;
import imd.ufrn.br.extensions.ExtensionManager;
import imd.ufrn.br.gateway.HTTPGateway;
import imd.ufrn.br.infra.MetricsCollector;
import imd.ufrn.br.infra.MetricsExporter;
import imd.ufrn.br.lifecycle.Lifecycle;
import imd.ufrn.br.lifecycle.LifecycleManager;
import imd.ufrn.br.monitoring.HeartbeatMonitor;
import imd.ufrn.br.registry.RouteRegistry;
import imd.ufrn.br.remoting.AsyncInvoker;
import imd.ufrn.br.remoting.Invoker;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

public class MiddlewarePlatform {

    private MiddlewareConfig config;
    private ExtensionManager extensionManager;
    private HTTPGateway httpGateway;
    private MetricsExporter metricsExporter;
    private RouteRegistry routeRegistry;
    private LifecycleManager lifecycleManager;
    private HeartbeatMonitor heartbeatMonitor;
    private Broker broker;
    private AsyncInvoker asyncInvoker;

    private boolean isRunning = false;
    private final CountDownLatch shutdownLatch = new CountDownLatch(1);

    public MiddlewarePlatform() {
        // Constructor is light, all work is in start()
    }

    public void start(String[] args) throws IOException {
        if (isRunning) {
            return;
        }

        System.out.println("Iniciando plataforma middleware...");

        config = new MiddlewareConfig();
        config.overrideFromCommandLine(args);

        System.out.println("Configuração carregada:");
        System.out.println("  Porta HTTP: " + config.getHttpPort());
        System.out.println("  Modo assíncrono: " + (config.isAsyncEnabled() ? "habilitado" : "desabilitado"));
        System.out.println("  Métricas: " + (config.isMetricsEnabled() ? "habilitadas" : "desabilitadas"));

        extensionManager = new ExtensionManager();
        routeRegistry = RouteRegistry.getInstance();
        lifecycleManager = new LifecycleManager();

        heartbeatMonitor = new HeartbeatMonitor(5000, 2000, 3);
        heartbeatMonitor.setEndpoint(config.getServerHost(), config.getHttpPort());

        Invoker invoker = new Invoker();
        
        if (config.isAsyncEnabled()) {
            asyncInvoker = new AsyncInvoker(config.getAsyncPoolSize());
        }

        MetricsCollector metricsCollector = null;
        if (config.isMetricsEnabled()) {
            metricsCollector = new MetricsCollector();
            if (config.isMetricsExportEnabled()) {
                metricsExporter = new MetricsExporter(metricsCollector);
            }
        }

        if (config.isAsyncEnabled() && asyncInvoker != null) {
            broker = new Broker(invoker, asyncInvoker, extensionManager, metricsCollector, true, config.getAsyncTimeout());
        } else {
            broker = new Broker(invoker, extensionManager, metricsCollector);
        }
        
        httpGateway = new HTTPGateway(routeRegistry, broker);

        if (httpGateway instanceof Lifecycle) {
            lifecycleManager.register((Lifecycle) httpGateway);
        }
        if (heartbeatMonitor instanceof Lifecycle) {
            lifecycleManager.register((Lifecycle) heartbeatMonitor);
        }
        if (metricsExporter instanceof Lifecycle) {
            lifecycleManager.register((Lifecycle) metricsExporter);
        }

        try {
            httpGateway.start(config.getHttpPort());
            System.out.println("Gateway HTTP iniciado na porta " + config.getHttpPort());

            if (metricsExporter != null) {
                metricsExporter.start(config.getMetricsExportPort());
                System.out.println("Métricas disponíveis na porta " + config.getMetricsExportPort());
            }

            heartbeatMonitor.start();

        } catch (Exception e) {
            throw new IOException("Failed to start components", e);
        }

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                stop();
            } catch (IOException e) {
                System.err.println("Error during shutdown: " + e.getMessage());
            }
        }, "MiddlewareShutdownHook"));

        isRunning = true;
        System.out.println("Plataforma middleware iniciada com sucesso.");
    }

    public void stop() throws IOException {
        if (!isRunning) {
            return;
        }
        System.out.println("Parando plataforma middleware...");

        lifecycleManager.stopAll();
        
        if (broker != null) {
            broker.shutdown();
        }

        isRunning = false;
        shutdownLatch.countDown();
        System.out.println("Plataforma middleware parada.");
    }

    public void awaitShutdown() throws InterruptedException {
        shutdownLatch.await();
    }

    public void registerService(Object serviceInstance) {
        if (routeRegistry == null) {
            System.err.println("Erro: O registro de rotas não foi inicializado.");
            return;
        }
        
        routeRegistry.register(serviceInstance);
        
        Class<?> serviceClass = serviceInstance.getClass();
        String serviceName = serviceClass.getSimpleName();
        
        if (serviceClass.isAnnotationPresent(RequestMapping.class)) {
            RequestMapping mapping = serviceClass.getAnnotation(RequestMapping.class);
            String path = mapping.path();
            if (path != null && !path.isEmpty()) {
                serviceName = path.startsWith("/") ? path.substring(1) : path;
            }
        }
        
        if (serviceInstance instanceof Lifecycle) {
            lifecycleManager.register((Lifecycle) serviceInstance);
        }
        
        System.out.println("Serviço '" + serviceName + "' registrado");
    }

    public void registerExtension(Extension extension) {
        if (extensionManager != null) {
            extensionManager.addExtension(extension);
        } else {
            System.err.println("Erro: O gerenciador de extensões não foi inicializado.");
        }
    }
    
    public MiddlewareConfig getConfig() {
        return config;
    }
}