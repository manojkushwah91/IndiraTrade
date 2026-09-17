package com.indiratrading.controller;

import com.indiratrading.service.LoadTestGenerator;
import com.indiratrading.service.PerformanceMetricsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/loadtest")
public class LoadTestController {

    @Autowired private LoadTestGenerator loadTestGenerator;
    @Autowired private PerformanceMetricsService metricsService;

    @PostMapping("/run")
    public ResponseEntity<Map<String, Object>> runLoadTest(
            @RequestParam(defaultValue = "100000") int ledgerRows,
            @RequestParam(defaultValue = "10000") int holdings,
            @RequestParam(defaultValue = "2000") int concurrentReaders) {
        try {
            long start = System.currentTimeMillis();
            Map<String, Object> results = loadTestGenerator.generateLoadTest(
                ledgerRows, holdings, concurrentReaders);
            long elapsed = System.currentTimeMillis() - start;
            results.put("generationTimeMs", elapsed);
            metricsService.recordQueryTime("load_test", elapsed);
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", e.getMessage()
            ));
        }
    }
}
