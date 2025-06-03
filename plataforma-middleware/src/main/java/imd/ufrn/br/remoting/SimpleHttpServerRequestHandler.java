package imd.ufrn.br.remoting;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import imd.ufrn.br.broker.Broker;
import imd.ufrn.br.identification.LookupService;
import imd.ufrn.br.identification.ObjectId;
import imd.ufrn.br.exceptions.MarshallingException;
import imd.ufrn.br.exceptions.ObjectNotFoundException;
import imd.ufrn.br.exceptions.RemotingException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.Executors;

/**
 * A simple HTTP server request handler that uses Java's built-in HttpServer.
 * It listens for HTTP requests, unmarshals them, passes them to a {@link Broker},
 * receives the result, marshals it, and sends it back as an HTTP response.
 *
 * Expected URL format: /invoke/{objectName}/{methodName}
 * Request body: JSON array of parameters, e.g., [10, "hello"]
 * Method: POST
 */
public class SimpleHttpServerRequestHandler implements HttpHandler {

    private final Broker broker;
    private final JsonMarshaller marshaller;
    private final LookupService lookupService;

    private static final String DEFAULT_CONTENT_TYPE = "application/json; charset=UTF-8";
    private static final String BASE_PATH = "/invoke";

    /**
     * Constructs a new SimpleHttpServerRequestHandler.
     *
     * @param broker        The {@link Broker} to delegate requests to.
     * @param marshaller    The {@link JsonMarshaller} for data conversion.
     * @param lookupService The {@link LookupService} to find objects and inspect methods.
     */
    public SimpleHttpServerRequestHandler(Broker broker, JsonMarshaller marshaller, LookupService lookupService) {
        if (broker == null || marshaller == null || lookupService == null) {
            throw new IllegalArgumentException("Broker, Marshaller, and LookupService cannot be null.");
        }
        this.broker = broker;
        this.marshaller = marshaller;
        this.lookupService = lookupService;
    }

    /**
     * Starts the HTTP server on the specified port.
     *
     * @param port The port number to listen on.
     * @throws IOException if an I/O error occurs when creating or starting the server.
     */
    public void start(int port) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext(BASE_PATH, this);
        server.setExecutor(Executors.newCachedThreadPool());
        server.start();
        System.out.println("SimpleHttpServerRequestHandler: HTTP server started on port " + port +
                ". Listening on context path: " + BASE_PATH);
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String httpMethod = exchange.getRequestMethod();

        System.out.println("SimpleHttpServerRequestHandler: Received request: " + httpMethod + " " + path);

        try {
            if (!"POST".equalsIgnoreCase(httpMethod)) {
                sendErrorResponse(exchange, 405, "Method Not Allowed", "Only POST method is supported.");
                return;
            }

            String[] pathParts = path.split("/");
            if (pathParts.length != 4 || !BASE_PATH.substring(1).equals(pathParts[1])) {
                sendErrorResponse(exchange, 400, "Bad Request", "Invalid path format. Use " + BASE_PATH + "/{objectName}/{methodName}");
                return;
            }

            String objectName = decodeUrlPart(pathParts[2]);
            String methodName = decodeUrlPart(pathParts[3]);
            ObjectId objectId = new ObjectId(objectName);

            String requestBody;
            try (InputStream is = exchange.getRequestBody()) {
                requestBody = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            }
            System.out.println("SimpleHttpServerRequestHandler: Request body for " + objectName + "#" + methodName + ": " + requestBody);


            Object targetObject = lookupService.findObject(objectId);
            int potentialParamCount = 0;
            if (requestBody != null && !requestBody.trim().isEmpty() && !"[]".equals(requestBody.trim())) {

                try {
                    List<Object> tempList = marshaller.unmarshal(requestBody, List.class);
                    if (tempList != null) potentialParamCount = tempList.size();
                } catch (MarshallingException e) {
                    System.err.println("SimpleHttpServerRequestHandler: Preliminary param count check failed: " + e.getMessage());
                }
            }


            Method actualMethod = findActualMethod(targetObject.getClass(), methodName, potentialParamCount, requestBody);
            if (actualMethod == null) {
                throw new NoSuchMethodException("No suitable method '" + methodName + "' found in " +
                        targetObject.getClass().getName() + " that matches provided parameters.");
            }
            Class<?>[] paramTypes = actualMethod.getParameterTypes();

            Object[] params = marshaller.unmarshalParameters(requestBody, paramTypes);

            Object result = broker.processRequest(objectId, methodName, params);
            String jsonResponse = marshaller.marshal(result);

            sendSuccessResponse(exchange, jsonResponse);

        } catch (ObjectNotFoundException e) {
            System.err.println("SimpleHttpServerRequestHandler: Object not found - " + e.getMessage());
            sendErrorResponse(exchange, 404, "Not Found", e.getMessage());
        } catch (NoSuchMethodException e) {
            System.err.println("SimpleHttpServerRequestHandler: Method not found - " + e.getMessage());
            sendErrorResponse(exchange, 404, "Method Not Found", e.getMessage());
        } catch (MarshallingException e) {
            System.err.println("SimpleHttpServerRequestHandler: Marshalling error - " + e.getMessage());
            sendErrorResponse(exchange, 400, "Bad Request", "Error processing request data: " + e.getMessage());
        } catch (RemotingException e) {
            System.err.println("SimpleHttpServerRequestHandler: Remoting platform error - " + e.getMessage());
            sendErrorResponse(exchange, 500, "Internal Server Error", "Platform error: " + e.getMessage());
        } catch (Throwable e) {
            System.err.println("SimpleHttpServerRequestHandler: Unhandled error during request processing - " + e.getClass().getName() + ": " + e.getMessage());
            e.printStackTrace();
            String errorMessage = e.getMessage();
            if (e.getCause() != null) {
                errorMessage = e.getCause().getClass().getSimpleName() + ": " + e.getCause().getMessage();
            }
            sendErrorResponse(exchange, 500, "Internal Server Error", "Error during method execution: " + errorMessage);
        } finally {
            exchange.close();
        }
    }

    /**
     * Helper to find a method, trying to be a bit more robust with parameter count.
     * Tries to find a method that matches name and number of parameters.
     * If multiple such methods exist (overloads), this simple approach might pick the first one.
     * A more sophisticated version would try to match types more closely after a generic unmarshal.
     */
    private Method findActualMethod(Class<?> targetClass, String methodName, int numParamsFromJson, String jsonBody) throws MarshallingException {
        List<Method> candidates = new ArrayList<>();
        for (Method method : targetClass.getMethods()) {
            if (method.getName().equals(methodName)) {
                candidates.add(method);
            }
        }

        if (candidates.isEmpty()) {
            return null;
        }

        if (candidates.size() == 1) {
            if (numParamsFromJson != -1 && candidates.get(0).getParameterCount() != numParamsFromJson) {
                System.err.println("SimpleHttpServerRequestHandler: Single candidate method " + methodName +
                        " param count " + candidates.get(0).getParameterCount() +
                        " does not match hint from JSON " + numParamsFromJson);
            }
            return candidates.get(0);
        }

        if (numParamsFromJson != -1) {
            for (Method candidate : candidates) {
                if (candidate.getParameterCount() == numParamsFromJson) {
                    return candidate;
                }
            }
        }

        if (candidates.stream().map(Method::getParameterCount).distinct().count() > 1 && numParamsFromJson == -1) {
            throw new MarshallingException("Ambiguous method '" + methodName + "'. Multiple overloads exist and parameter count cannot be determined from request.", null);
        }
        return candidates.get(0);
    }


    private void sendSuccessResponse(HttpExchange exchange, String jsonResponseBody) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", DEFAULT_CONTENT_TYPE);
        exchange.sendResponseHeaders(200, jsonResponseBody.getBytes(StandardCharsets.UTF_8).length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(jsonResponseBody.getBytes(StandardCharsets.UTF_8));
        }
    }

    private void sendErrorResponse(HttpExchange exchange, int statusCode, String errorType, String errorMessage) throws IOException {
        Map<String, String> errorMap = new HashMap<>();
        errorMap.put("error", errorType);
        errorMap.put("message", errorMessage);
        String jsonErrorBody;
        try {
            jsonErrorBody = marshaller.marshal(errorMap);
        } catch (MarshallingException e) {
            jsonErrorBody = "{\"error\":\"InternalServerError\",\"message\":\"Failed to marshal error response.\"}";
            statusCode = 500;
        }

        exchange.getResponseHeaders().set("Content-Type", DEFAULT_CONTENT_TYPE);
        exchange.sendResponseHeaders(statusCode, jsonErrorBody.getBytes(StandardCharsets.UTF_8).length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(jsonErrorBody.getBytes(StandardCharsets.UTF_8));
        }
    }

    private String decodeUrlPart(String part) {
        try {
            return URLDecoder.decode(part, StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            System.err.println("SimpleHttpServerRequestHandler: Error decoding URL part (should not happen with UTF-8): " + part);
            return part;
        }
    }
}