package com.indiratrading.service;

import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Service
public class PerformanceMetricsService {

    private final ConcurrentHashMap<String, List<Long>> queryTimes = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicLong> counters = new ConcurrentHashMap<>();
    private final AtomicLong totalImports = new AtomicLong(0);
    private final AtomicLong totalCases = new AtomicLong(0);
    private final AtomicLong totalQueries = new AtomicLong(0);

    public void recordQueryTime(String operation, long timeMs) {
        queryTimes.computeIfAbsent(operation, k -> new ArrayList<>()).add(timeMs);
        totalQueries.incrementAndGet();
    }

    public void incrementCounter(String name) {
        counters.computeIfAbsent(name, k -> new AtomicLong(0)).incrementAndGet();
    }

    public void incrementImports() { totalImports.incrementAndGet(); }
    public void incrementCases() { totalCases.incrementAndGet(); }

    public Map<String, Object> getMetrics() {
        Map<String, Object> metrics = new HashMap<>();

        // Counters
        metrics.put("totalImports", totalImports.get());
        metrics.put("totalCases", totalCases.get());
        metrics.put("totalQueries", totalQueries.get());

        // Per-operation counters
        Map<String, Long> counterMap = new HashMap<>();
        counters.forEach((k, v) -> counterMap.put(k, v.get()));
        metrics.put("counters", counterMap);

        // Query performance
        Map<String, Map<String, Long>> performance = new HashMap<>();
        queryTimes.forEach((operation, times) -> {
            if (!times.isEmpty()) {
                List<Long> sorted = times.stream().sorted().collect(Collectors.toList());
                Map<String, Long> stats = new HashMap<>();
                stats.put("count", (long) sorted.size());
                stats.put("p50", sorted.get(sorted.size() / 2));
                stats.put("p95", sorted.get((int) (sorted.size() * 0.95)));
                stats.put("p99", sorted.get((int) (sorted.size() * 0.99)));
                stats.put("min", sorted.get(0));
                stats.put("max", sorted.get(sorted.size() - 1));
                stats.put("avg", sorted.stream().mapToLong(Long::longValue).sum() / sorted.size());
                performance.put(operation, stats);
            }
        });
        metrics.put("performance", performance);

        // Hardware info
        Runtime runtime = Runtime.getRuntime();
        Map<String, Object> hardware = new HashMap<>();
        hardware.put("availableProcessors", runtime.availableProcessors());
        hardware.put("maxMemoryMB", runtime.maxMemory() / (1024 * 1024));
        hardware.put("totalMemoryMB", runtime.totalMemory() / (1024 * 1024));
        hardware.put("freeMemoryMB", runtime.freeMemory() / (1024 * 1024));
        metrics.put("hardware", hardware);

        return metrics;
    }

    public Map<String, Object> getHealthCheck() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("timestamp", System.currentTimeMillis());
        health.put("uptimeSeconds", getUptime());
        return health;
    }

    private long getUptime() {
        return java.lang.management.ManagementFactory.getRuntimeMXBean().getUptime() / 1000;
    }
}
