package com.Life_ledger.controller;

import com.Life_ledger.entity.RecurringPattern;
import com.Life_ledger.service.RecurringPatternService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/recurring")
@RequiredArgsConstructor
public class RecurringPatternController {

    private final RecurringPatternService recurringPatternService;

    @PostMapping
    public ResponseEntity<RecurringPattern> create(@RequestBody RecurringPattern recurringPattern) {
        return ResponseEntity.ok(recurringPatternService.createRecurringPattern(recurringPattern));
    }

    @GetMapping("/{id}")
    public ResponseEntity<RecurringPattern> getOne(@PathVariable Long id) {
        return ResponseEntity.ok(recurringPatternService.getRecurringPattern(id));
    }

    @GetMapping
    public ResponseEntity<List<RecurringPattern>> getAll() {
        return ResponseEntity.ok(recurringPatternService.getAllRecurringPatterns());
    }

    @PutMapping("/{id}")
    public ResponseEntity<RecurringPattern> update(
            @PathVariable Long id,
            @RequestBody RecurringPattern recurringPattern) {

        return ResponseEntity.ok(recurringPatternService.updateRecurringPattern(id, recurringPattern));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> delete(@PathVariable Long id) {
        recurringPatternService.deleteRecurringPattern(id);
        return ResponseEntity.ok("Recurring Pattern deleted successfully");
    }
}
