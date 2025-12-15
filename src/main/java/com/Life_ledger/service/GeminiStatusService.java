package com.Life_ledger.service;

import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

import com.Life_ledger.Enum.GeminiStep;
import com.Life_ledger.Enum.GeminiTaskStatus;
import com.fasterxml.jackson.databind.JsonNode;

@Service
public class GeminiStatusService {

    private final Map<String, EnumMap<GeminiStep, GeminiTaskStatus>> store = new ConcurrentHashMap<>();

    public EnumMap<GeminiStep, GeminiTaskStatus> getOrCreate(String key) {
        return store.computeIfAbsent(key, k -> {
            EnumMap<GeminiStep, GeminiTaskStatus> map = new EnumMap<>(GeminiStep.class);
            for (GeminiStep s : GeminiStep.values()) {
                map.put(s, new GeminiTaskStatus());
            }
            return map;
        });
    }

    public void update(
            String key,
            GeminiStep step,
            GeminiTaskStatus.State state,
            String message,
            JsonNode result) {

        GeminiTaskStatus status = getOrCreate(key).get(step);
        status.setState(state);
        status.setMessage(message);
        status.setResult(result);
        status.setUpdatedAt(LocalDateTime.now());
    }

    public GeminiTaskStatus get(String key, GeminiStep step) {
        return getOrCreate(key).get(step);
    }
}
