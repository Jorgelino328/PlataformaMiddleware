package imd.ufrn.br.gateway;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import imd.ufrn.br.annotations.HttpVerb;
import imd.ufrn.br.broker.Broker;
import imd.ufrn.br.lifecycle.Lifecycle;
import imd.ufrn.br.registry.RouteInfo;
import imd.ufrn.br.registry.RouteRegistry;
import imd.ufrn.br.remoting.JsonMarshaller;
import imd.ufrn.br.remoting.Request;
import imd.ufrn.br.remoting.Response;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;

public class HTTPGateway implements HttpHandler, Lifecycle {

    private final RouteRegistry routeRegistry;
    private final Broker broker;
    private final JsonMarshaller marshaller;
    private HttpServer server;
    private volatile boolean running = false;

    public HTTPGateway(RouteRegistry routeRegistry, Broker broker) {
        this.routeRegistry = routeRegistry;
        this.broker = broker;
        this.marshaller = new JsonMarshaller(); // Using a concrete instance for now
    }

    public void start(int httpPort) throws IOException {
        if (running) {
            return;
        }
        server = HttpServer.create(new InetSocketAddress(httpPort), 0);
        server.createContext("/", this);
        server.setExecutor(Executors.newCachedThreadPool());
        server.start();
        running = true;
    }

    @Override
    public void start() throws Exception {
        start(8082);
    }

    @Override
    public void stop() throws Exception {
        if (server != null && running) {
            server.stop(0);
            running = false;
        }
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String methodStr = exchange.getRequestMethod();
        
        if (path.startsWith("/health")) {
            handleHealthCheck(exchange, path);
            return;
        }
        
        HttpVerb verb;

        try {
            verb = HttpVerb.valueOf(methodStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            sendErrorResponse(exchange, 405, "Method Not Allowed", "HTTP verb '" + methodStr + "' is not supported.");
            return;
        }

        try {
            RouteInfo route = routeRegistry.findRoute(verb, path);

            if (route == null) {
                sendErrorResponse(exchange, 404, "Not Found", "No route found for " + verb + " " + path);
                return;
            }

            String requestBody;
            try (InputStream is = exchange.getRequestBody()) {
                requestBody = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            }

            Object[] params = marshaller.unmarshalParameters(requestBody, route.parameterTypes());

            Request brokerRequest = new Request(route.instance(), route.method(), params);

            Response brokerResponse = broker.invoke(brokerRequest);

            if (brokerResponse.hasError()) {
                sendErrorResponse(exchange, 500, "Internal Server Error", brokerResponse.getErrorMessage());
            } else {
                String jsonResponse = marshaller.serialize(brokerResponse.getResult());
                sendSuccessResponse(exchange, jsonResponse);
            }

        } catch (Exception e) {
            System.err.println("HTTPGateway: Error processing request - " + e.getMessage());
            sendErrorResponse(exchange, 500, "Internal Server Error", "Gateway error: " + e.getMessage());
        } finally {
            exchange.close();
        }
    }
    
    private void handleHealthCheck(HttpExchange exchange, String path) throws IOException {
        try {
            if (path.equals("/health") || path.equals("/health/")) {
                String response = "{\"status\":\"UP\",\"gateway\":\"HTTPGateway\",\"timestamp\":" + System.currentTimeMillis() + "}";
                sendSuccessResponse(exchange, response);
            } else {
                String[] parts = path.split("/");
                if (parts.length >= 3) {
                    String serviceName = parts[2];
                    boolean serviceExists = routeRegistry.getAllServiceNames().contains(serviceName);
                    
                    if (serviceExists) {
                        String response = "{\"status\":\"UP\",\"service\":\"" + serviceName + "\",\"timestamp\":" + System.currentTimeMillis() + "}";
                        sendSuccessResponse(exchange, response);
                    } else {
                        sendErrorResponse(exchange, 404, "Not Found", "Service '" + serviceName + "' not found");
                    }
                } else {
                    sendErrorResponse(exchange, 400, "Bad Request", "Invalid health check path");
                }
            }
        } finally {
            exchange.close();
        }
    }

    private void sendSuccessResponse(HttpExchange exchange, String responseBody) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        byte[] responseBytes = responseBody.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(200, responseBytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(responseBytes);
        }
    }

    private void sendErrorResponse(HttpExchange exchange, int statusCode, String errorType, String errorMessage) throws IOException {
        String sanitizedMessage = errorMessage != null ? errorMessage.replace("\"", "'") : "null";
        String jsonErrorBody = "{\"error\":\"" + errorType + "\",\"message\":\"" + sanitizedMessage + "\"}";
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        byte[] responseBytes = jsonErrorBody.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(statusCode, responseBytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(responseBytes);
        }
    }
}
