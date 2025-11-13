package imd.ufrn.br.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class MiddlewareConfig {
    
    private final Properties properties;
    
    public MiddlewareConfig() {
        this.properties = new Properties();
        loadDefaultProperties();
    }
    
    public MiddlewareConfig(String configFile) {
        this.properties = new Properties();
        loadPropertiesFromFile(configFile);
        loadDefaultProperties(); // Fallback for missing properties
    }
    
    private void loadDefaultProperties() {
        properties.putIfAbsent("server.http.port", "8082");
        properties.putIfAbsent("server.tcp.port", "8085");
        properties.putIfAbsent("server.udp.port", "8086");
        properties.putIfAbsent("server.host", "localhost");
        
        properties.putIfAbsent("async.enabled", "false");
        properties.putIfAbsent("async.poolsize", "10");
        properties.putIfAbsent("async.timeout.ms", "30000");
        
        properties.putIfAbsent("async.threadpool.size", "8");
        properties.putIfAbsent("udp.threadpool.size", "20");
        properties.putIfAbsent("tcp.threadpool.size", "10");
        
        properties.putIfAbsent("heartbeat.interval.ms", "5000");
        properties.putIfAbsent("heartbeat.timeout.ms", "3000");
        properties.putIfAbsent("heartbeat.max.missed", "3");
        
        properties.putIfAbsent("metrics.enabled", "true");
        properties.putIfAbsent("metrics.export.enabled", "false");
        properties.putIfAbsent("metrics.export.port", "9090");
        
        properties.putIfAbsent("discovery.enabled", "true");
        properties.putIfAbsent("discovery.registry.ttl.ms", "30000");
    }
    
    private void loadPropertiesFromFile(String configFile) {
        try (InputStream is = getClass().getResourceAsStream(configFile)) {
            if (is != null) {
                properties.load(is);
            }
        } catch (IOException e) {
            System.err.println("MiddlewareConfig: Could not load " + configFile);
        }
    }
    
    public void overrideFromCommandLine(String[] args) {
        for (String arg : args) {
            if (arg.startsWith("--")) {
                String[] parts = arg.substring(2).split("=", 2);
                if (parts.length == 2) {
                    String key = parts[0].replace("-", ".");
                    String value = parts[1];
                    properties.setProperty(key, value);
                }
            }
        }
    }
    
    public int getHttpPort() { return Integer.parseInt(properties.getProperty("server.http.port")); }
    public int getTcpPort() { return Integer.parseInt(properties.getProperty("server.tcp.port")); }
    public int getUdpPort() { return Integer.parseInt(properties.getProperty("server.udp.port")); }
    public String getServerHost() { return properties.getProperty("server.host"); }
    
    public boolean isAsyncEnabled() { return Boolean.parseBoolean(properties.getProperty("async.enabled")); }
    public int getAsyncPoolSize() { return Integer.parseInt(properties.getProperty("async.poolsize")); }
    public long getAsyncTimeout() { return Long.parseLong(properties.getProperty("async.timeout.ms")); }
    
    public int getAsyncThreadPoolSize() { return Integer.parseInt(properties.getProperty("async.threadpool.size")); }
    public int getUdpThreadPoolSize() { return Integer.parseInt(properties.getProperty("udp.threadpool.size")); }
    public int getTcpThreadPoolSize() { return Integer.parseInt(properties.getProperty("tcp.threadpool.size")); }
    
    public long getHeartbeatIntervalMs() { return Long.parseLong(properties.getProperty("heartbeat.interval.ms")); }
    public long getHeartbeatTimeoutMs() { return Long.parseLong(properties.getProperty("heartbeat.timeout.ms")); }
    public int getMaxMissedHeartbeats() { return Integer.parseInt(properties.getProperty("heartbeat.max.missed")); }
    
    public boolean isMetricsEnabled() { return Boolean.parseBoolean(properties.getProperty("metrics.enabled")); }
    public boolean isMetricsExportEnabled() { return Boolean.parseBoolean(properties.getProperty("metrics.export.enabled")); }
    public int getMetricsExportPort() { return Integer.parseInt(properties.getProperty("metrics.export.port")); }
    
    public boolean isDiscoveryEnabled() { return Boolean.parseBoolean(properties.getProperty("discovery.enabled")); }
    public long getRegistryTtlMs() { return Long.parseLong(properties.getProperty("discovery.registry.ttl.ms")); }
    
    public String getProperty(String key) { return properties.getProperty(key); }
    public String getProperty(String key, String defaultValue) { return properties.getProperty(key, defaultValue); }
}