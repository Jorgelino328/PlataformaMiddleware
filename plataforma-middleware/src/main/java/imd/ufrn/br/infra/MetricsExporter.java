package imd.ufrn.br.infra;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import imd.ufrn.br.lifecycle.Lifecycle;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.Executors;

public class MetricsExporter implements HttpHandler, Lifecycle {
    
    private final MetricsCollector metricsCollector;
    private HttpServer server;
    private volatile boolean running = false;
    
    public MetricsExporter(MetricsCollector metricsCollector) {
        this.metricsCollector = metricsCollector;
    }
    
    public void start(int port) throws IOException {
        if (running) {
            return;
        }
        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/metrics", this);
        server.setExecutor(Executors.newSingleThreadExecutor());
        server.start();
        running = true;
    }
    
    @Override
    public void start() throws Exception {
        start(9090);
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
        if (!"GET".equals(exchange.getRequestMethod())) {
            sendResponse(exchange, 405, "Method Not Allowed");
            return;
        }
        
        StringBuilder response = new StringBuilder();
        response.append("# Middleware Metrics\n");
        response.append("# TYPE invocation_count counter\n");
        response.append("# TYPE invocation_latency_avg gauge\n");
        
        Map<String, MetricsCollector.Stats> allStats = metricsCollector.getAllStats();
        
        for (Map.Entry<String, MetricsCollector.Stats> entry : allStats.entrySet()) {
            String methodKey = entry.getKey();
            MetricsCollector.Stats stats = entry.getValue();
            
            String[] parts = methodKey.split("#");
            if (parts.length == 2) {
                String serviceName = parts[0];
                String methodName = parts[1];
                long count = stats.getCount();
                double avgLatency = stats.getAverageLatency();
                
                response.append(String.format("invocation_count{service=\"%s\",method=\"%s\"} %d\n", 
                                            serviceName, methodName, count));
                response.append(String.format("invocation_latency_avg{service=\"%s\",method=\"%s\"} %.2f\n", 
                                            serviceName, methodName, avgLatency));
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