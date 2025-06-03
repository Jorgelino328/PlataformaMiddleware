package imd.ufrn.br.app;

import imd.ufrn.br.annotations.RemoteObject;
import imd.ufrn.br.broker.Broker;
import imd.ufrn.br.identification.LookupService;
import imd.ufrn.br.identification.ObjectId;
import imd.ufrn.br.remoting.Invoker;
import imd.ufrn.br.remoting.JsonMarshaller;
import imd.ufrn.br.remoting.SimpleHttpServerRequestHandler;

import java.io.IOException;

public class Main {

    public static void main(String[] args) {
        System.out.println("Starting Sample Application...");

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

        SimpleHttpServerRequestHandler requestHandler =
                new SimpleHttpServerRequestHandler(broker, marshaller, lookupService); // Pass all three

        int port = 8080;
        try {
            requestHandler.start(port);
            System.out.println("Application Server is running on port " + port);
            System.out.println("Access remote objects via HTTP POST to: http://localhost:" + port + "/invoke/{objectName}/{methodName}");
            System.out.println("Example for add: POST http://localhost:" + port + "/invoke/" + serviceName + "/add with body [10,20]");
            System.out.println("Example for echo: POST http://localhost:" + port + "/invoke/" + serviceName + "/echo with body [\"Hello World\"]");
        } catch (IOException e) {
            System.err.println("Fatal: Failed to start HTTP server on port " + port + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
}