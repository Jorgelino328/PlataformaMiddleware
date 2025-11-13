package imd.ufrn.br.servidor.middleware.management;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class HeartbeatMonitor {

    private final Map<String, Boolean> serviceStatus = new HashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public void registerService(String serviceName) {
        serviceStatus.put(serviceName, true); // Assume service is healthy on registration
        System.out.println("Service registered for Heartbeat monitoring: " + serviceName);
    }

    public void startMonitoring() {
        System.out.println("Heartbeat monitor started.");
        scheduler.scheduleAtFixedRate(this::checkServices, 5, 15, TimeUnit.SECONDS);
    }

    public void stopMonitoring() {
        System.out.println("Heartbeat monitor stopped.");
        scheduler.shutdown();
    }

    private void checkServices() {
        System.out.println("Performing heartbeat check...");
        for (String serviceName : serviceStatus.keySet()) {
            // In a real scenario, this would involve a health check mechanism (e.g., pinging an endpoint)
            boolean isHealthy = Math.random() > 0.1; // Simulate a 10% chance of failure
            serviceStatus.put(serviceName, isHealthy);
            System.out.println("  - " + serviceName + " is " + (isHealthy ? "healthy" : "unhealthy"));
        }
    }

    public Map<String, Boolean> getServiceStatus() {
        return new HashMap<>(serviceStatus);
    }
}
