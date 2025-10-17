package imd.ufrn.br.infra;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

/**
 * Minimal metrics collector for invocation counts and average latency.
 */
public class MetricsCollector {

    private static class Stats {
        final LongAdder count = new LongAdder();
        final LongAdder totalLatencyMs = new LongAdder();
    }

    private final Map<String, Stats> stats = new ConcurrentHashMap<>();

    public void record(String objectId, String methodName, long latencyMs) {
        String key = objectId + "#" + methodName;
        Stats s = stats.computeIfAbsent(key, k -> new Stats());
        s.count.increment();
        s.totalLatencyMs.add(latencyMs);
    }

    public long getCount(String objectId, String methodName) {
        Stats s = stats.get(objectId + "#" + methodName);
        return s == null ? 0L : s.count.longValue();
    }

    public double getAverageLatency(String objectId, String methodName) {
        Stats s = stats.get(objectId + "#" + methodName);
        if (s == null || s.count.longValue() == 0) return 0.0;
        return (double) s.totalLatencyMs.longValue() / s.count.longValue();
    }
}
