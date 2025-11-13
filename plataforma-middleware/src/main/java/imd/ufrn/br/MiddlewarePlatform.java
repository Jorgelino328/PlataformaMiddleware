package imd.ufrn.br;

import imd.ufrn.br.broker.Broker;
import imd.ufrn.br.config.MiddlewareConfig;
import imd.ufrn.br.extensions.Extension;
import imd.ufrn.br.extensions.ExtensionManager;
import imd.ufrn.br.gateway.HTTPGateway;
import imd.ufrn.br.infra.MetricsCollector;
import imd.ufrn.br.infra.MetricsExporter;
import imd.ufrn.br.registry.RouteRegistry;
import imd.ufrn.br.remoting.Invoker;
import imd.ufrn.br.servidor.middleware.management.HeartbeatMonitor;
import imd.ufrn.br.servidor.middleware.management.LifecycleManager;

import java.io.IOException;

public class MiddlewarePlatform {

    private MiddlewareConfig config;
    private ExtensionManager extensionManager;
    private HTTPGateway httpGateway;
    private MetricsExporter metricsExporter;
    private RouteRegistry routeRegistry;
    private LifecycleManager lifecycleManager;
    private HeartbeatMonitor heartbeatMonitor;

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
        System.out.println("  Métricas: " + (config.isMetricsEnabled() ? "habilitadas" : "desabilitadas"));

        extensionManager = new ExtensionManager();
        routeRegistry = RouteRegistry.getInstance();
        lifecycleManager = new LifecycleManager();
        heartbeatMonitor = new HeartbeatMonitor();


        Invoker invoker = new Invoker();

        MetricsCollector metricsCollector = null;
        if (config.isMetricsEnabled()) {
            metricsCollector = new MetricsCollector();
            if (config.isMetricsExportEnabled()) {
                metricsExporter = new MetricsExporter(metricsCollector);
            }
        }

        Broker broker = new Broker(invoker, extensionManager, metricsCollector);
        httpGateway = new HTTPGateway(routeRegistry, broker);

        lifecycleManager.registerComponent(httpGateway);
        if (metricsExporter != null) {
            lifecycleManager.registerComponent(metricsExporter);
        }
        lifecycleManager.registerComponent(heartbeatMonitor);

        lifecycleManager.startAll();

        httpGateway.start(config.getHttpPort());
        System.out.println("Gateway HTTP iniciado com sucesso na porta " + config.getHttpPort());

        if (metricsExporter != null) {
            metricsExporter.start(config.getMetricsExportPort());
            System.out.println("Exportador de métricas iniciado na porta " + config.getMetricsExportPort());
        }

        heartbeatMonitor.startMonitoring();

        isRunning = true;
        System.out.println("Plataforma middleware iniciada com sucesso.");
        System.out.println("Endpoint HTTP JMeter: http://" + config.getServerHost() + ":" + config.getHttpPort() + "/invoke/{objectName}/{methodName}");
        if (metricsExporter != null) {
            System.out.println("Endpoint de métricas: http://" + config.getServerHost() + ":" + config.getMetricsExportPort() + "/metrics");
        }
        System.out.println("Exemplo de requisição JMeter: POST http://" + config.getServerHost() + ":" + config.getHttpPort() + "/invoke/CalculatorService/add com body [10,20]");
        System.out.println("Pressione Ctrl+C para parar.");
    }

    public void stop() throws IOException {
        if (!isRunning) {
            return;
        }
        System.out.println("Parando plataforma middleware...");

        lifecycleManager.stopAll();

        if (httpGateway != null) {
            httpGateway.stop();
        }
        if (metricsExporter != null) {
            metricsExporter.stop();
        }

        isRunning = false;
        System.out.println("Plataforma middleware parada.");
    }

    public void registerService(Object serviceInstance) {
        if (routeRegistry != null) {
            routeRegistry.register(serviceInstance);
            heartbeatMonitor.registerService(serviceInstance.getClass().getSimpleName());
        } else {
            System.err.println("Erro: O registro de rotas não foi inicializado.");
        }
    }

    public void registerExtension(Extension extension) {
        if (extensionManager != null) {
            extensionManager.addExtension(extension);
        } else {
            System.err.println("Erro: O gerenciador de extensões não foi inicializado.");
        }
    }
}