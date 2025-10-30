package imd.ufrn.br.discovery;

import imd.ufrn.br.identification.AbsoluteObjectReference;
import imd.ufrn.br.identification.ObjectId;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Service registry that maintains AbsoluteObjectReferences for distributed services.
 * Supports TTL-based expiration and automatic cleanup.
 */
public class ServiceRegistry {
    
    private static class ServiceEntry {
        final AbsoluteObjectReference reference;
        volatile long lastUpdated;
        final long ttlMs;
        
        ServiceEntry(AbsoluteObjectReference reference, long ttlMs) {
            this.reference = reference;
            this.lastUpdated = System.currentTimeMillis();
            this.ttlMs = ttlMs;
        }
        
        boolean isExpired() {
            return (System.currentTimeMillis() - lastUpdated) > ttlMs;
        }
        
        void refresh() {
            this.lastUpdated = System.currentTimeMillis();
        }
    }
    
    private final Map<ObjectId, ServiceEntry> registry = new ConcurrentHashMap<>();
    private final ScheduledExecutorService cleanupExecutor = Executors.newSingleThreadScheduledExecutor();
    private final long defaultTtlMs;
    private boolean started = false;
    
    public ServiceRegistry(long defaultTtlMs) {
        this.defaultTtlMs = defaultTtlMs;
    }
    
    public void start() {
        if (!started) {
            // Run cleanup every 10 seconds
            cleanupExecutor.scheduleAtFixedRate(this::cleanupExpiredEntries, 10, 10, TimeUnit.SECONDS);
            started = true;
            System.out.println("ServiceRegistry: Started with TTL=" + defaultTtlMs + "ms");
        }
    }
    
    public void stop() {
        if (started) {
            cleanupExecutor.shutdown();
            started = false;
            System.out.println("ServiceRegistry: Stopped");
        }
    }
    
    public void register(ObjectId objectId, String host, int port) {
        register(objectId, host, port, defaultTtlMs);
    }
    
    public void register(ObjectId objectId, String host, int port, long ttlMs) {
        AbsoluteObjectReference reference = new AbsoluteObjectReference(objectId, host, port);
        ServiceEntry entry = new ServiceEntry(reference, ttlMs);
        registry.put(objectId, entry);
        System.out.println("ServiceRegistry: Registered " + objectId.getId() + " at " + host + ":" + port);
    }
    
    public void heartbeat(ObjectId objectId) {
        ServiceEntry entry = registry.get(objectId);
        if (entry != null) {
            entry.refresh();
        }
    }
    
    public AbsoluteObjectReference discover(ObjectId objectId) {
        ServiceEntry entry = registry.get(objectId);
        if (entry != null && !entry.isExpired()) {
            return entry.reference;
        }
        return null;
    }
    
    public boolean unregister(ObjectId objectId) {
        ServiceEntry removed = registry.remove(objectId);
        if (removed != null) {
            System.out.println("ServiceRegistry: Unregistered " + objectId.getId());
            return true;
        }
        return false;
    }
    
    private void cleanupExpiredEntries() {
        int removed = 0;
        for (Map.Entry<ObjectId, ServiceEntry> entry : registry.entrySet()) {
            if (entry.getValue().isExpired()) {
                registry.remove(entry.getKey());
                removed++;
                System.out.println("ServiceRegistry: Expired entry for " + entry.getKey().getId());
            }
        }
        if (removed > 0) {
            System.out.println("ServiceRegistry: Cleaned up " + removed + " expired entries");
        }
    }
    
    public int getRegistrySize() {
        return registry.size();
    }
}