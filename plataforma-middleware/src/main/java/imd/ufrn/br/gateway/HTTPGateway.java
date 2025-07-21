package imd.ufrn.br.gateway;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;

public class HTTPGateway implements HttpHandler {
    
    private final String tcpServerHost;
    private final int tcpServerPort;

    public HTTPGateway(String tcpServerHost, int tcpServerPort) {
        this.tcpServerHost = tcpServerHost;
        this.tcpServerPort = tcpServerPort;
    }

    public void start(int httpPort) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(httpPort), 0);
        server.createContext("/invoke", this);
        server.setExecutor(Executors.newCachedThreadPool());
        server.start();
        System.out.println("HTTPGateway: Started on port " + httpPort + ", forwarding to TCP server at " + tcpServerHost + ":" + tcpServerPort);
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String httpMethod = exchange.getRequestMethod();

        System.out.println("HTTPGateway: Received request: " + httpMethod + " " + path);

        try {
            if (!"POST".equalsIgnoreCase(httpMethod)) {
                sendErrorResponse(exchange, 405, "Method Not Allowed", "Only POST method is supported.");
                return;
            }

            String[] pathParts = path.split("/");
            if (pathParts.length != 4 || !"invoke".equals(pathParts[1])) {
                sendErrorResponse(exchange, 400, "Bad Request", "Invalid path format. Use /invoke/{objectName}/{methodName}");
                return;
            }

            String objectName = pathParts[2];
            String methodName = pathParts[3];

            String requestBody;
            try (InputStream is = exchange.getRequestBody()) {
                requestBody = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            }

            System.out.println("HTTPGateway: Forwarding request to TCP server: " + objectName + "|" + methodName + "|" + requestBody);

            String tcpRequest = objectName + "|" + methodName + "|" + requestBody;
            String tcpResponse = forwardToTCPServer(tcpRequest);

            if (tcpResponse != null && tcpResponse.startsWith("ERROR:")) {
                sendErrorResponse(exchange, 500, "Internal Server Error", tcpResponse);
            } else {
                sendSuccessResponse(exchange, tcpResponse != null ? tcpResponse : "null");
            }

        } catch (Exception e) {
            System.err.println("HTTPGateway: Error processing request - " + e.getMessage());
            e.printStackTrace();
            sendErrorResponse(exchange, 500, "Internal Server Error", "Gateway error: " + e.getMessage());
        } finally {
            exchange.close();
        }
    }

    private String forwardToTCPServer(String request) {
        try (
            Socket socket = new Socket(tcpServerHost, tcpServerPort);
            PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))
        ) {
            socket.setSoTimeout(5000);
            
            out.println(request);
            String response = in.readLine();
            
            System.out.println("HTTPGateway: Received response from TCP server: " + response);
            return response;
            
        } catch (IOException e) {
            System.err.println("HTTPGateway: Error forwarding to TCP server - " + e.getMessage());
            return "ERROR: TCP server communication failed - " + e.getMessage();
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
        String jsonErrorBody = "{\"error\":\"" + errorType + "\",\"message\":\"" + errorMessage + "\"}";
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        byte[] responseBytes = jsonErrorBody.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(statusCode, responseBytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(responseBytes);
        }
    }
}
