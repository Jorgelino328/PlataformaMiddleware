package imd.ufrn.br;

import imd.ufrn.br.annotations.RequestMapping;
import imd.ufrn.br.broker.Broker;
import imd.ufrn.br.config.MiddlewareConfig;
import imd.ufrn.br.discovery.ServiceRegistry;
import imd.ufrn.br.extensions.Extension;
import imd.ufrn.br.extensions.ExtensionManager;
import imd.ufrn.br.gateway.HTTPGateway;
import imd.ufrn.br.identification.LookupService;
import imd.ufrn.br.identification.ObjectId;
import imd.ufrn.br.infra.MetricsCollector;
import imd.ufrn.br.infra.MetricsExporter;
import imd.ufrn.br.lifecycle.LifecycleManager;
import imd.ufrn.br.monitoring.HeartbeatMonitor;
import imd.ufrn.br.registry.RouteRegistry;
import imd.ufrn.br.remoting.AsyncInvoker;
import imd.ufrn.br.remoting.Invoker;

import java.io.IOException;

public class MiddlewarePlatform {

    private MiddlewareConfig config;
    private LookupService lookupService;
    private ServiceRegistry serviceRegistry;
    private ExtensionManager extensionManager;
    private LifecycleManager lifecycleManager;
    private AsyncInvoker asyncInvoker;
    private HTTPGateway httpGateway;
    private HeartbeatMonitor heartbeatMonitor;
    private MetricsExporter metricsExporter;
    private RouteRegistry routeRegistry;

    private boolean isRunning = false;

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
        System.out.println("  Porta TCP: " + config.getTcpPort());
        System.out.println("  Porta UDP: " + config.getUdpPort());
        System.out.println("  Pool Assíncrono: " + config.getAsyncThreadPoolSize() + " threads");
        System.out.println("  Métricas: " + (config.isMetricsEnabled() ? "habilitadas" : "desabilitadas"));
        
        lookupService = LookupService.getInstance();
        extensionManager = new ExtensionManager();
        lifecycleManager = new LifecycleManager(lookupService);
        routeRegistry = RouteRegistry.getInstance();

        lookupService.setExtensionManager(extensionManager);
        lookupService.setLifecycleManager(lifecycleManager);

        serviceRegistry = new ServiceRegistry(config.getRegistryTtlMs());
        Invoker invoker = new Invoker();
        asyncInvoker = new AsyncInvoker(config.getAsyncThreadPoolSize());

        MetricsCollector metricsCollector = null;
        if (config.isMetricsEnabled()) {
            metricsCollector = new MetricsCollector();
            if (config.isMetricsExportEnabled()) {
                metricsExporter = new MetricsExporter(metricsCollector);
            }
        }

        Broker broker = new Broker(invoker, extensionManager, metricsCollector);
        httpGateway = new HTTPGateway(routeRegistry, broker);

        heartbeatMonitor = new HeartbeatMonitor(
                config.getHeartbeatIntervalMs(),
                config.getHeartbeatTimeoutMs(),
                config.getMaxMissedHeartbeats()
        );

        serviceRegistry.start();
        lifecycleManager.startAll();

        httpGateway.start(config.getHttpPort());
        System.out.println("Gateway HTTP iniciado com sucesso na porta " + config.getHttpPort());

        // Heartbeat for TCP/UDP servers is disabled as they are no longer the primary entry point.
        heartbeatMonitor.start();
        System.out.println("Monitor de heartbeat iniciado com sucesso.");

        if (metricsExporter != null) {
            metricsExporter.start(config.getMetricsExportPort());
            System.out.println("Exportador de métricas iniciado na porta " + config.getMetricsExportPort());
        }

        Runtime.getRuntime().addShutdownHook(new Thread(this::stop));

        isRunning = true;
        System.out.println("Aplicação pronta.");
        System.out.println("Endpoint HTTP JMeter: http://" + config.getServerHost() + ":" + config.getHttpPort() + "/invoke/{objectName}/{methodName}");
        System.out.println("Endpoint TCP (middleware): " + config.getServerHost() + ":" + config.getTcpPort());
        System.out.println("Endpoint UDP (tolerância a falhas): " + config.getServerHost() + ":" + config.getUdpPort());
        if (metricsExporter != null) {
            System.out.println("Endpoint de métricas: http://" + config.getServerHost() + ":" + config.getMetricsExportPort() + "/metrics");
        }
        System.out.println("Exemplo de requisição JMeter: POST http://" + config.getServerHost() + ":" + config.getHttpPort() + "/invoke/CalculatorService/add com body [10,20]");
        System.out.println("Pressione Ctrl+C para parar.");
    }

    public void stop() {
        if (!isRunning) {
            return;
        }
        isRunning = false;
        
        System.out.println("\nEncerrando graciosamente...");
        heartbeatMonitor.stop();
        asyncInvoker.shutdown();
        lifecycleManager.stopAll();
        serviceRegistry.stop();
        if (metricsExporter != null) {
            metricsExporter.stop();
        }
    }

    public void registerRemoteObject(Object serviceInstance) {
        if (!isRunning) {
            throw new IllegalStateException("Middleware platform is not running. Call start() first.");
        }
        if (serviceInstance == null) {
            throw new IllegalArgumentException("Service instance cannot be null.");
        }

        Class<?> serviceClass = serviceInstance.getClass();
        if (!serviceClass.isAnnotationPresent(RequestMapping.class)) {
            throw new IllegalArgumentException("Object " + serviceClass.getName() + " is not a valid service. It must be annotated with @RequestMapping.");
        }

        routeRegistry.register(serviceInstance);
    }

    public void registerExtension(Extension extension) {
         if (extensionManager != null) {
            extensionManager.registerExtension(extension);
         }
    }

    public void awaitShutdown() {
        if (!isRunning) {
            return;
        }
        try {
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Main thread interrupted, initiating shutdown.");
            stop();
        }
    }
}