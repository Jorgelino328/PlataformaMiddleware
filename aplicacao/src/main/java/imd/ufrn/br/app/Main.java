package imd.ufrn.br.app;

import imd.ufrn.br.annotations.RemoteObject;
import imd.ufrn.br.broker.Broker;
import imd.ufrn.br.identification.LookupService;
import imd.ufrn.br.identification.ObjectId;
import imd.ufrn.br.remoting.Invoker;
import imd.ufrn.br.remoting.JsonMarshaller;
import imd.ufrn.br.remoting.UDPServerRequestHandler;
import imd.ufrn.br.remoting.TCPServerRequestHandler;
import imd.ufrn.br.gateway.HTTPGateway;
import imd.ufrn.br.monitoring.HeartbeatMonitor;

import java.io.IOException;

public class Main {

    public static void main(String[] args) {
        System.out.println("Starting distributed middleware application with UDP...");

        LookupService lookupService = LookupService.getInstance();
        Invoker invoker = new Invoker(lookupService);
        Broker broker = new Broker(invoker);
        JsonMarshaller marshaller = new JsonMarshaller();

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
        } catch (IllegalArgumentException e) {
            System.err.println("Error registering object " + serviceName + ": " + e.getMessage());
            return;
        }

        UDPServerRequestHandler udpHandler =
                new UDPServerRequestHandler(broker, marshaller, lookupService);

        TCPServerRequestHandler tcpHandler =
                new TCPServerRequestHandler(broker, marshaller, lookupService);

        HTTPGateway httpGateway = new HTTPGateway("localhost", 8085);

        HeartbeatMonitor heartbeatMonitor = new HeartbeatMonitor();

        int httpPort = 8082;
        int tcpPort = 8085;
        int udpPort = 8086;
        try {
            tcpHandler.start(tcpPort);
            System.out.println("TCP server started successfully on port " + tcpPort);
            
            httpGateway.start(httpPort);
            System.out.println("HTTP Gateway started successfully on port " + httpPort + " (for JMeter)");
            
            udpHandler.start(udpPort);
            System.out.println("UDP server started successfully on port " + udpPort);

            heartbeatMonitor.start();
            System.out.println("Heartbeat monitor started successfully.");

            System.out.println("Application ready.");
            System.out.println("JMeter HTTP endpoint: http://localhost:" + httpPort + "/invoke/{objectName}/{methodName}");
            System.out.println("TCP endpoint (middleware): localhost:" + tcpPort);
            System.out.println("UDP endpoint (fault tolerance): localhost:" + udpPort);
            System.out.println("Example JMeter request: POST http://localhost:" + httpPort + "/invoke/" + serviceName + "/add with body [10,20]");
            System.out.println("Press Ctrl+C to stop.");

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("\nShutting down gracefully...");
                heartbeatMonitor.stop();
                tcpHandler.stop();
                udpHandler.stop();
            }));

            Thread.currentThread().join();
        } catch (IOException | InterruptedException e) {
            System.err.println("Error starting servers: " + e.getMessage());
            e.printStackTrace();
        }
    }
}