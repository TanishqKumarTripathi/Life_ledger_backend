package com.Life_ledger.controller;

import com.Life_ledger.entity.Insight;
import com.Life_ledger.service.InsightService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;



@RestController
@RequestMapping("/insights")
@RequiredArgsConstructor
public class InsightController {

    private final InsightService insightService;

    @PostMapping
    public ResponseEntity<Insight> create(@RequestBody Insight insight) {
        return ResponseEntity.ok(insightService.createInsight(insight));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Insight> getOne(@PathVariable Long id) {
        return ResponseEntity.ok(insightService.getInsight(id));
    }

    @GetMapping
    public ResponseEntity<List<Insight>> getAll() {
        return ResponseEntity.ok(insightService.getAllInsights());
    }

    @PutMapping("/{id}")
    public ResponseEntity<Insight> update(
            @PathVariable Long id,
            @RequestBody Insight insight) {

        return ResponseEntity.ok(insightService.updateInsight(id, insight));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> delete(@PathVariable Long id) {
        insightService.deleteInsight(id);
        return ResponseEntity.ok("Insight deleted successfully");
    }
}