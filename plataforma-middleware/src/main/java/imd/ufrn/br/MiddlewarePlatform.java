package imd.ufrn.br;

import imd.ufrn.br.annotations.RemoteObject;
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
import imd.ufrn.br.remoting.*;

import java.io.IOException;

/**
 * The main facade for the middleware platform.
 * This class encapsulates all the bootstrap logic and component wiring,
 * hiding it from the end-user application.
 */
public class MiddlewarePlatform {

    private MiddlewareConfig config;
    private LookupService lookupService;
    private ServiceRegistry serviceRegistry;
    private ExtensionManager extensionManager;
    private LifecycleManager lifecycleManager;
    private AsyncInvoker asyncInvoker;
    private TCPServerRequestHandler tcpHandler;
    private UDPServerRequestHandler udpHandler;
    private HTTPGateway httpGateway;
    private HeartbeatMonitor heartbeatMonitor;
    private MetricsExporter metricsExporter;

    private boolean isRunning = false;

    public MiddlewarePlatform() {
        // Constructor is light, all work is in start()
    }

    /**
     * Starts and initializes all middleware components.
     * @param args Command-line arguments for configuration overrides.
     * @throws IOException if any server fails to start.
     */
    public void start(String[] args) throws IOException {
        if (isRunning) {
            return;
        }

        System.out.println("Iniciando plataforma middleware...");

        // 1. Load configuration
        config = new MiddlewareConfig();
        config.overrideFromCommandLine(args);

        System.out.println("Configuração carregada:");
        System.out.println("  Porta HTTP: " + config.getHttpPort());
        System.out.println("  Porta TCP: " + config.getTcpPort());
        System.out.println("  Porta UDP: " + config.getUdpPort());
        System.out.println("  Pool Assíncrono: " + config.getAsyncThreadPoolSize() + " threads");
        System.out.println("  Métricas: " + (config.isMetricsEnabled() ? "habilitadas" : "desabilitadas"));
        
        // 2. Initialize Core Services (Singleton/Managers)
        lookupService = LookupService.getInstance();
        extensionManager = new ExtensionManager();
        lifecycleManager = new LifecycleManager(lookupService);

        // Wire them up
        lookupService.setExtensionManager(extensionManager);
        lookupService.setLifecycleManager(lifecycleManager);

        // 3. Initialize Functional Components
        serviceRegistry = new ServiceRegistry(config.getRegistryTtlMs());
        Invoker invoker = new Invoker(lookupService);
        asyncInvoker = new AsyncInvoker(lookupService, config.getAsyncThreadPoolSize());

        MetricsCollector metricsCollector = null;
        if (config.isMetricsEnabled()) {
            metricsCollector = new MetricsCollector();
            if (config.isMetricsExportEnabled()) {
                metricsExporter = new MetricsExporter(metricsCollector);
            }
        }

        Broker broker = new Broker(invoker, asyncInvoker, extensionManager, metricsCollector);
        JsonMarshaller marshaller = new JsonMarshaller();

        // 4. Initialize Handlers and Gateways
        udpHandler = new UDPServerRequestHandler(broker, marshaller, lookupService, config.getUdpThreadPoolSize());
        tcpHandler = new TCPServerRequestHandler(broker, marshaller, lookupService, config.getTcpThreadPoolSize());
        httpGateway = new HTTPGateway(serviceRegistry, config.getServerHost(), config.getTcpPort());

        heartbeatMonitor = new HeartbeatMonitor(
                config.getHeartbeatIntervalMs(),
                config.getHeartbeatTimeoutMs(),
                config.getMaxMissedHeartbeats()
        );

        // 5. Start all services
        serviceRegistry.start();
        lifecycleManager.startAll();

        // Start servers
        tcpHandler.start(config.getTcpPort());
        System.out.println("Servidor TCP iniciado com sucesso na porta " + config.getTcpPort());
        
        httpGateway.start(config.getHttpPort());
        System.out.println("Gateway HTTP iniciado com sucesso na porta " + config.getHttpPort() + " (para JMeter)");
        
        udpHandler.start(config.getUdpPort());
        System.out.println("Servidor UDP iniciado com sucesso na porta " + config.getUdpPort());

        // Auto-register internal components for monitoring
        heartbeatMonitor.registerComponent("TCPServer", config.getServerHost(), config.getTcpPort() + 1000);
        heartbeatMonitor.registerComponent("UDPServer", config.getServerHost(), config.getUdpPort());
        heartbeatMonitor.start();
        System.out.println("Monitor de heartbeat iniciado com sucesso.");

        if (metricsExporter != null) {
            metricsExporter.start(config.getMetricsExportPort());
            System.out.println("Exportador de métricas iniciado na porta " + config.getMetricsExportPort());
        }

        // 6. Register a single shutdown hook for the entire platform
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

    /**
     * Gracefully stops all middleware components.
     */
    public void stop() {
        if (!isRunning) {
            return;
        }
        isRunning = false;
        
        System.out.println("\nEncerrando graciosamente...");
        heartbeatMonitor.stop();
        tcpHandler.stop();
        udpHandler.stop();
        asyncInvoker.shutdown();
        lifecycleManager.stopAll();
        serviceRegistry.stop();
        if (metricsExporter != null) {
            metricsExporter.stop();
        }
    }

    /**
     * Registers an application-provided service object with the middleware.
     * This method automatically handles ID creation and service discovery.
     *
     * @param serviceInstance The object instance to be registered (must be annotated with @RemoteObject).
     */
    public void registerRemoteObject(Object serviceInstance) {
        if (!isRunning) {
            throw new IllegalStateException("Middleware platform is not running. Call start() first.");
        }
        if (serviceInstance == null) {
            throw new IllegalArgumentException("Service instance cannot be null.");
        }

        RemoteObject annotation = serviceInstance.getClass().getAnnotation(RemoteObject.class);
        if (annotation == null) {
            throw new IllegalArgumentException("Object " + serviceInstance.getClass().getName() + " is not annotated with @RemoteObject.");
        }

        String serviceName = annotation.name().isEmpty() ? serviceInstance.getClass().getSimpleName() : annotation.name();
        ObjectId objectId = new ObjectId(serviceName);

        try {
            // Register with local lookup
            lookupService.registerObject(objectId, serviceInstance);
            
            // Automatically register with service discovery
            serviceRegistry.register(objectId, config.getServerHost(), config.getTcpPort());
            System.out.println("Serviço '" + serviceName + "' registrado com sucesso.");
        
        } catch (Exception e) {
            System.err.println("Erro ao registrar objeto remoto: " + e.getMessage());
        }
    }

    /**
     * Registers an application-provided extension with the middleware.
     *
     * @param extension The extension implementation to register.
     */
    public void registerExtension(Extension extension) {
         if (extensionManager != null) {
            extensionManager.registerExtension(extension);
         }
    }

    /**
     * Blocks the calling thread (typically the main thread) to keep the
     * platform running until it is interrupted.
     */
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
