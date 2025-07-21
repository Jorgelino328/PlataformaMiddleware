package imd.ufrn.br.monitoring;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class HeartbeatMonitor {
    
    private final Map<String, ComponentInfo> components = new ConcurrentHashMap<>();
    private final Map<String, Integer> missedHeartbeats = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    
    private static final int HEARTBEAT_INTERVAL_MS = 5000;
    private static final int HEARTBEAT_TIMEOUT_MS = 3000;
    private static final int MAX_MISSED_HEARTBEATS = 3;
    
    private boolean running = false;

    public static class ComponentInfo {
        private final String name;
        private final String host;
        private final int port;
        private boolean healthy;

        public ComponentInfo(String name, String host, int port) {
            this.name = name;
            this.host = host;
            this.port = port;
            this.healthy = true;
        }

        public String getName() { return name; }
        public String getHost() { return host; }
        public int getPort() { return port; }
        public boolean isHealthy() { return healthy; }
        public void setHealthy(boolean healthy) { this.healthy = healthy; }
    }

    public void registerComponent(String name, String host, int port) {
        ComponentInfo component = new ComponentInfo(name, host, port);
        components.put(name, component);
        missedHeartbeats.put(name, 0);
        System.out.println("HeartbeatMonitor: Registered component " + name + " at " + host + ":" + port);
    }

    public void start() {
        if (running) {
            return;
        }
        
        running = true;
        System.out.println("HeartbeatMonitor: Starting heartbeat monitoring...");
        
        scheduler.scheduleAtFixedRate(
            this::checkAllComponents, 
            HEARTBEAT_INTERVAL_MS, 
            HEARTBEAT_INTERVAL_MS, 
            TimeUnit.MILLISECONDS
        );
    }

    public void stop() {
        if (!running) {
            return;
        }
        
        running = false;
        System.out.println("HeartbeatMonitor: Stopping heartbeat monitoring...");
        
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

    private void checkAllComponents() {
        for (ComponentInfo component : components.values()) {
            if (!component.isHealthy()) {
                continue;
            }
            
            boolean isAlive = checkComponentHealth(component);
            
            if (!isAlive) {
                int missed = missedHeartbeats.getOrDefault(component.getName(), 0) + 1;
                missedHeartbeats.put(component.getName(), missed);
                
                if (missed >= MAX_MISSED_HEARTBEATS) {
                    System.err.println("HeartbeatMonitor: Component " + component.getName() + 
                                     " is dead after " + missed + " missed heartbeats");
                    component.setHealthy(false);
                    missedHeartbeats.remove(component.getName());
                } else {
                    System.out.println("HeartbeatMonitor: Component " + component.getName() + 
                                     " missed heartbeat (" + missed + "/" + MAX_MISSED_HEARTBEATS + ")");
                }
            } else {
                missedHeartbeats.put(component.getName(), 0);
                if (!component.isHealthy()) {
                    component.setHealthy(true);
                    System.out.println("HeartbeatMonitor: Component " + component.getName() + " recovered");
                }
            }
        }
    }

    private boolean checkComponentHealth(ComponentInfo component) {
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.setSoTimeout(HEARTBEAT_TIMEOUT_MS);
            
            String message = "HEARTBEAT";
            byte[] sendData = message.getBytes();
            
            InetAddress address = InetAddress.getByName(component.getHost());
            DatagramPacket sendPacket = new DatagramPacket(
                sendData, sendData.length, address, component.getPort()
            );
            socket.send(sendPacket);
            
            byte[] receiveData = new byte[1024];
            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
            
            try {
                socket.receive(receivePacket);
                String response = new String(receivePacket.getData(), 0, receivePacket.getLength());
                return "HEARTBEAT_ACK".equals(response);
            } catch (SocketTimeoutException e) {
                return false;
            }
        } catch (IOException e) {
            System.err.println("HeartbeatMonitor: Error checking component health - " + e.getMessage());
            return false;
        }
    }

    public Map<String, ComponentInfo> getComponents() {
        return components;
    }
}
