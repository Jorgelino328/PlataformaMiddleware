package imd.ufrn.br.gateway;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import imd.ufrn.br.annotations.HttpVerb;
import imd.ufrn.br.broker.Broker;
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

public class HTTPGateway implements HttpHandler {

    private final RouteRegistry routeRegistry;
    private final Broker broker;
    private final JsonMarshaller marshaller;

    public HTTPGateway(RouteRegistry routeRegistry, Broker broker) {
        this.routeRegistry = routeRegistry;
        this.broker = broker;
        this.marshaller = new JsonMarshaller(); // Using a concrete instance for now
    }

    public void start(int httpPort) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(httpPort), 0);
        // All paths are now dynamic, handled by a single handler at the root.
        server.createContext("/", this);
        server.setExecutor(Executors.newCachedThreadPool());
        server.start();
        System.out.println("HTTPGateway: Started on port " + httpPort + ". All requests will be routed dynamically.");
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String methodStr = exchange.getRequestMethod();
        HttpVerb verb;

        try {
            verb = HttpVerb.valueOf(methodStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            sendErrorResponse(exchange, 405, "Method Not Allowed", "HTTP verb '" + methodStr + "' is not supported.");
            return;
        }

        System.out.println("HTTPGateway: Received request: " + verb + " " + path);

        try {
            RouteInfo route = routeRegistry.findRoute(verb, path);

            if (route == null) {
                sendErrorResponse(exchange, 404, "Not Found", "No route found for " + verb + " " + path);
                return;
            }

            // The service is local, invoke directly via the Broker
            String requestBody;
            try (InputStream is = exchange.getRequestBody()) {
                requestBody = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            }

            // Deserialize parameters
            Object[] params = marshaller.unmarshalParameters(requestBody, route.parameterTypes());

            // Create a request for the broker
            Request brokerRequest = new Request(route.instance(), route.method(), params);

            // Invoke and get the response
            Response brokerResponse = broker.invoke(brokerRequest);

            if (brokerResponse.hasError()) {
                sendErrorResponse(exchange, 500, "Internal Server Error", brokerResponse.getErrorMessage());
            } else {
                String jsonResponse = marshaller.serialize(brokerResponse.getResult());
                sendSuccessResponse(exchange, jsonResponse);
            }

        } catch (Exception e) {
            System.err.println("HTTPGateway: Error processing request - " + e.getMessage());
            e.printStackTrace();
            sendErrorResponse(exchange, 500, "Internal Server Error", "Gateway error: " + e.getMessage());
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
        // Sanitize error message for JSON
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
