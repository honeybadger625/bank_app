package com.example.banking.controller;

import com.example.banking.service.SimulatorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/simulate")
public class SimulateController {

    @Autowired
    private SimulatorService simulatorService;

    @PostMapping("/start")
    public ResponseEntity<?> start(@RequestBody Map<String, Object> body) {
        int tx = (int) ((Number) body.getOrDefault("transactions", 100)).intValue();
        int minDelay = (int) ((Number) body.getOrDefault("minDelayMs", 10)).intValue();
        int maxDelay = (int) ((Number) body.getOrDefault("maxDelayMs", 100)).intValue();
        String runId = simulatorService.startSimulation(tx, minDelay, maxDelay);
        return ResponseEntity.accepted().body(Map.of("runId", runId));
    }

    @GetMapping("/status/{runId}")
    public ResponseEntity<?> status(@PathVariable String runId) {
        String s = simulatorService.getStatus(runId);
        return ResponseEntity.ok(Map.of("runId", runId, "status", s));
    }
}
