package imd.ufrn.br.app;

import imd.ufrn.br.annotations.RemoteObject;
import imd.ufrn.br.broker.Broker;
import imd.ufrn.br.config.MiddlewareConfig;
import imd.ufrn.br.discovery.ServiceRegistry;
import imd.ufrn.br.extensions.ExtensionManager;
import imd.ufrn.br.extensions.Extension;
import imd.ufrn.br.gateway.HTTPGateway;
import imd.ufrn.br.identification.LookupService;
import imd.ufrn.br.identification.ObjectId;
import imd.ufrn.br.infra.MetricsCollector;
import imd.ufrn.br.infra.MetricsExporter;
import imd.ufrn.br.lifecycle.LifecycleManager;
import imd.ufrn.br.monitoring.HeartbeatMonitor;
import imd.ufrn.br.remoting.AsyncInvoker;
import imd.ufrn.br.remoting.Invoker;
import imd.ufrn.br.remoting.JsonMarshaller;
import imd.ufrn.br.remoting.TCPServerRequestHandler;
import imd.ufrn.br.remoting.UDPServerRequestHandler;

import java.io.IOException;

public class Main {

    public static void main(String[] args) {
        System.out.println("Starting distributed middleware application...");

        // Load configuration
        MiddlewareConfig config = new MiddlewareConfig();
        config.overrideFromCommandLine(args);
        
        System.out.println("Configuration loaded:");
        System.out.println("  HTTP Port: " + config.getHttpPort());
        System.out.println("  TCP Port: " + config.getTcpPort());
        System.out.println("  UDP Port: " + config.getUdpPort());
        System.out.println("  Async Pool: " + config.getAsyncThreadPoolSize() + " threads");
        System.out.println("  Metrics: " + (config.isMetricsEnabled() ? "enabled" : "disabled"));

        LookupService lookupService = LookupService.getInstance();
        Invoker invoker = new Invoker(lookupService);

        // Extension and lifecycle managers
        ExtensionManager extensionManager = new ExtensionManager();
        LifecycleManager lifecycleManager = new LifecycleManager(lookupService);
        lookupService.setExtensionManager(extensionManager);
        lookupService.setLifecycleManager(lifecycleManager);

        // Service registry for discovery
        ServiceRegistry serviceRegistry = new ServiceRegistry(config.getRegistryTtlMs());
        serviceRegistry.start();

        // Async invoker
        AsyncInvoker asyncInvoker = new AsyncInvoker(lookupService, config.getAsyncThreadPoolSize());

        // Metrics (if enabled)
        MetricsCollector metricsCollector = null;
        final MetricsExporter metricsExporter;
        if (config.isMetricsEnabled()) {
            metricsCollector = new MetricsCollector();
            if (config.isMetricsExportEnabled()) {
                metricsExporter = new MetricsExporter(metricsCollector);
            } else {
                metricsExporter = null;
            }
        } else {
            metricsExporter = null;
        }

        // Wire broker with all capabilities
        Broker broker = new Broker(invoker, asyncInvoker, extensionManager, metricsCollector);
        JsonMarshaller marshaller = new JsonMarshaller();

        // Create and register calculator service
        CalculatorServiceImpl calculator = new CalculatorServiceImpl();
        RemoteObject remoteObjectAnnotation = calculator.getClass().getAnnotation(RemoteObject.class);
        String serviceName;
        if (remoteObjectAnnotation != null && !remoteObjectAnnotation.name().isEmpty()) {
            serviceName = remoteObjectAnnotation.name();
        } else {
            serviceName = calculator.getClass().getSimpleName();
            System.err.println("Warning: @RemoteObject name not specified for " +
                    calculator.getClass().getName() + ", using class name: " + serviceName);
        }

        ObjectId calculatorId = new ObjectId(serviceName);
        try {
            lookupService.registerObject(calculatorId, calculator);
            // Register in service registry
            serviceRegistry.register(calculatorId, config.getServerHost(), config.getTcpPort());
        } catch (IllegalArgumentException e) {
            System.err.println("Error registering object " + serviceName + ": " + e.getMessage());
            return;
        }

        // Register extension for logging
        extensionManager.registerExtension(new Extension() {
            @Override
            public void onInvoke(String objectId, String methodName) {
                System.out.println("[Extension] Invoking " + objectId + "#" + methodName);
            }
        });

        // Start lifecycle-managed components
        lifecycleManager.startAll();

        // Create server handlers with configurable thread pools
        UDPServerRequestHandler udpHandler = new UDPServerRequestHandler(
                broker, marshaller, lookupService, config.getUdpThreadPoolSize());

        TCPServerRequestHandler tcpHandler = new TCPServerRequestHandler(
                broker, marshaller, lookupService, config.getTcpThreadPoolSize());

        // HTTP Gateway with service discovery
        HTTPGateway httpGateway = new HTTPGateway(serviceRegistry, config.getServerHost(), config.getTcpPort());

        // Heartbeat monitor with configurable parameters
        HeartbeatMonitor heartbeatMonitor = new HeartbeatMonitor(
                config.getHeartbeatIntervalMs(),
                config.getHeartbeatTimeoutMs(),
                config.getMaxMissedHeartbeats());

        // Register components for heartbeat monitoring
        heartbeatMonitor.registerComponent("TCPServer", config.getServerHost(), config.getTcpPort() + 1000); // TCP heartbeat port
        heartbeatMonitor.registerComponent("UDPServer", config.getServerHost(), config.getUdpPort());

        try {
            // Start all servers
            tcpHandler.start(config.getTcpPort());
            System.out.println("TCP server started successfully on port " + config.getTcpPort());
            
            httpGateway.start(config.getHttpPort());
            System.out.println("HTTP Gateway started successfully on port " + config.getHttpPort() + " (for JMeter)");
            
            udpHandler.start(config.getUdpPort());
            System.out.println("UDP server started successfully on port " + config.getUdpPort());

            heartbeatMonitor.start();
            System.out.println("Heartbeat monitor started successfully.");

            // Start metrics exporter if enabled
            if (metricsExporter != null) {
                metricsExporter.start(config.getMetricsExportPort());
                System.out.println("Metrics exporter started on port " + config.getMetricsExportPort());
            }

            System.out.println("Application ready.");
            System.out.println("JMeter HTTP endpoint: http://" + config.getServerHost() + ":" + config.getHttpPort() + "/invoke/{objectName}/{methodName}");
            System.out.println("TCP endpoint (middleware): " + config.getServerHost() + ":" + config.getTcpPort());
            System.out.println("UDP endpoint (fault tolerance): " + config.getServerHost() + ":" + config.getUdpPort());
            if (metricsExporter != null) {
                System.out.println("Metrics endpoint: http://" + config.getServerHost() + ":" + config.getMetricsExportPort() + "/metrics");
            }
            System.out.println("Example JMeter request: POST http://" + config.getServerHost() + ":" + config.getHttpPort() + "/invoke/" + serviceName + "/add with body [10,20]");
            System.out.println("Press Ctrl+C to stop.");

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("\nShutting down gracefully...");
                heartbeatMonitor.stop();
                tcpHandler.stop();
                udpHandler.stop();
                asyncInvoker.shutdown();
                lifecycleManager.stopAll();
                serviceRegistry.stop();
                if (metricsExporter != null) {
                    metricsExporter.stop();
                }
            }));

            Thread.currentThread().join();
        } catch (IOException | InterruptedException e) {
            System.err.println("Error starting servers: " + e.getMessage());
            e.printStackTrace();
        }
    }
}