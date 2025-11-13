package imd.ufrn.br.monitoring;

import imd.ufrn.br.lifecycle.Lifecycle;
import imd.ufrn.br.registry.RouteRegistry;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.concurrent.*;

public class HeartbeatMonitor implements Lifecycle {
    
    private final long checkIntervalMs;
    private final long timeoutMs;
    private final int maxFailures;
    private final Map<String, ServiceHealth> serviceHealthMap = new ConcurrentHashMap<>();
    private ScheduledExecutorService scheduler;
    private volatile boolean running = false;
    private String baseUrl = "http://localhost";
    private int port = 8080;
    
    private static class ServiceHealth {
        String serviceName;
        boolean healthy = true;
        int consecutiveFailures = 0;
        long lastCheckTime = 0;
        String lastError = null;
        
        ServiceHealth(String serviceName) {
            this.serviceName = serviceName;
        }
    }
    
    public HeartbeatMonitor(long checkIntervalMs, long timeoutMs, int maxFailures) {
        this.checkIntervalMs = checkIntervalMs;
        this.timeoutMs = timeoutMs;
        this.maxFailures = maxFailures;
    }
    
    public void setEndpoint(String baseUrl, int port) {
        this.baseUrl = baseUrl;
        this.port = port;
    }
    
    public void registerService(String serviceName, String healthCheckPath) {
        ServiceHealth health = new ServiceHealth(serviceName);
        serviceHealthMap.put(serviceName, health);
    }
    
    @Override
    public void start() throws Exception {
        if (running) {
            return;
        }
        
        scheduler = Executors.newScheduledThreadPool(1, r -> {
            Thread t = new Thread(r, "HeartbeatMonitor");
            t.setDaemon(true);
            return t;
        });
        
        discoverServices();
        
        scheduler.scheduleAtFixedRate(
            this::checkAllServices,
            0,
            checkIntervalMs,
            TimeUnit.MILLISECONDS
        );
        
        running = true;
        System.out.println("HeartbeatMonitor started");
    }
    
    @Override
    public void stop() throws Exception {
        if (!running) {
            return;
        }
        
        if (scheduler != null) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        
        running = false;
    }
    
    @Override
    public boolean isRunning() {
        return running;
    }
    
    private void discoverServices() {
        RouteRegistry registry = RouteRegistry.getInstance();
        Set<String> serviceNames = registry.getAllServiceNames();
        
        for (String serviceName : serviceNames) {
            if (!serviceHealthMap.containsKey(serviceName)) {
                ServiceHealth health = new ServiceHealth(serviceName);
                serviceHealthMap.put(serviceName, health);
            }
        }
    }
    
    private void checkAllServices() {
        if (!running) {
            return;
        }
        
        discoverServices();
        
        for (ServiceHealth health : serviceHealthMap.values()) {
            checkService(health);
        }
    }
    
    private void checkService(ServiceHealth health) {
        health.lastCheckTime = System.currentTimeMillis();
        
        try {
            String healthCheckUrl = baseUrl + ":" + port + "/health/" + health.serviceName;
            
            HttpURLConnection connection = null;
            try {
                @SuppressWarnings("deprecation")
                URL url = new URL(healthCheckUrl);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout((int) timeoutMs);
                connection.setReadTimeout((int) timeoutMs);
                
                int responseCode = connection.getResponseCode();
                
                if (responseCode == 200) {
                    if (!health.healthy) {
                        System.out.println("Service '" + health.serviceName + "' recovered");
                    }
                    health.healthy = true;
                    health.consecutiveFailures = 0;
                    health.lastError = null;
                } else {
                    handleServiceFailure(health, "HTTP " + responseCode);
                }
            } catch (IOException e) {
                handleServiceFailure(health, e.getMessage());
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
            
        } catch (Exception e) {
            handleServiceFailure(health, "Health check error: " + e.getMessage());
        }
    }
    
    private void handleServiceFailure(ServiceHealth health, String errorMessage) {
        health.consecutiveFailures++;
        health.lastError = errorMessage;
        
        if (health.consecutiveFailures >= maxFailures && health.healthy) {
            health.healthy = false;
            System.err.println("Service '" + health.serviceName + 
                             "' marked as UNHEALTHY after " + maxFailures + 
                             " consecutive failures. Last error: " + errorMessage);
        }
    }
    
    public Map<String, Boolean> getHealthStatus() {
        Map<String, Boolean> status = new HashMap<>();
        for (Map.Entry<String, ServiceHealth> entry : serviceHealthMap.entrySet()) {
            status.put(entry.getKey(), entry.getValue().healthy);
        }
        return status;
    }
    
    public String getHealthReport() {
        StringBuilder report = new StringBuilder();
        report.append("=== Health Check Report ===\n");
        report.append("Total services: ").append(serviceHealthMap.size()).append("\n\n");
        
        for (ServiceHealth health : serviceHealthMap.values()) {
            report.append("Service: ").append(health.serviceName).append("\n");
            report.append("  Status: ").append(health.healthy ? "HEALTHY" : "UNHEALTHY").append("\n");
            report.append("  Consecutive failures: ").append(health.consecutiveFailures).append("\n");
            if (health.lastError != null) {
                report.append("  Last error: ").append(health.lastError).append("\n");
            }
            report.append("  Last check: ").append(new Date(health.lastCheckTime)).append("\n\n");
        }
        
        return report.toString();
    }
    
    public boolean isServiceHealthy(String serviceName) {
        ServiceHealth health = serviceHealthMap.get(serviceName);
        return health != null && health.healthy;
    }
}
