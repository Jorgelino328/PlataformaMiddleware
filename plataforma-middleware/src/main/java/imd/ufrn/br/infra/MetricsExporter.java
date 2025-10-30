package imd.ufrn.br.infra;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;

/**
 * HTTP endpoint for exporting metrics in a simple format.
 */
public class MetricsExporter implements HttpHandler {
    
    private final MetricsCollector metricsCollector;
    private HttpServer server;
    
    public MetricsExporter(MetricsCollector metricsCollector) {
        this.metricsCollector = metricsCollector;
    }
    
    public void start(int port) throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/metrics", this);
        server.setExecutor(Executors.newSingleThreadExecutor());
        server.start();
        System.out.println("MetricsExporter: Started on port " + port);
    }
    
    public void stop() {
        if (server != null) {
            server.stop(0);
            System.out.println("MetricsExporter: Stopped");
        }
    }
    
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) {
            sendResponse(exchange, 405, "Method Not Allowed");
            return;
        }
        
        StringBuilder response = new StringBuilder();
        response.append("# Middleware Metrics\n");
        response.append("# TYPE invocation_count counter\n");
        response.append("# TYPE invocation_latency_avg gauge\n");
        
        // Note: In a real implementation, we'd store method keys and iterate through them
        // For this demo, we'll show the concept with some sample methods
        String[] sampleMethods = {"CalculatorService#add", "CalculatorService#echo", "CalculatorService#getStatus"};
        
        for (String methodKey : sampleMethods) {
            String[] parts = methodKey.split("#");
            if (parts.length == 2) {
                long count = metricsCollector.getCount(parts[0], parts[1]);
                double avgLatency = metricsCollector.getAverageLatency(parts[0], parts[1]);
                
                if (count > 0) {
                    response.append(String.format("invocation_count{service=\"%s\",method=\"%s\"} %d\n", 
                                                parts[0], parts[1], count));
                    response.append(String.format("invocation_latency_avg{service=\"%s\",method=\"%s\"} %.2f\n", 
                                                parts[0], parts[1], avgLatency));
                }
            }
        }
        
        sendResponse(exchange, 200, response.toString());
    }
    
    private void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
        byte[] responseBytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(statusCode, responseBytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(responseBytes);
        }
    }
}