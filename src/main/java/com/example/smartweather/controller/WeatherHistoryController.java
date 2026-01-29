package com.example.smartweather.controller;

import com.example.smartweather.dto.WeatherHistoryDTO;
import com.example.smartweather.service.WeatherHistoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/weather-history")
@RequiredArgsConstructor
public class WeatherHistoryController {

    private final WeatherHistoryService weatherHistoryService;

    @GetMapping
    public ResponseEntity<List<WeatherHistoryDTO>> getAllHistory() {
        List<WeatherHistoryDTO> history = weatherHistoryService.getAllHistory();
        return ResponseEntity.ok(history);
    }

    @GetMapping("/{id}")
    public ResponseEntity<WeatherHistoryDTO> getHistoryById(@PathVariable Long id) {
        WeatherHistoryDTO history = weatherHistoryService.getHistoryById(id);
        return ResponseEntity.ok(history);
    }

    @GetMapping("/location/{locationId}")
    public ResponseEntity<List<WeatherHistoryDTO>> getHistoryByLocation(@PathVariable Long locationId) {
        List<WeatherHistoryDTO> history = weatherHistoryService.getHistoryByLocation(locationId);
        return ResponseEntity.ok(history);
    }

    @PostMapping
    public ResponseEntity<WeatherHistoryDTO> createHistory(@Valid @RequestBody WeatherHistoryDTO historyDTO) {
        WeatherHistoryDTO created = weatherHistoryService.createHistory(historyDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<WeatherHistoryDTO> updateHistory(
            @PathVariable Long id,
            @Valid @RequestBody WeatherHistoryDTO historyDTO) {
        WeatherHistoryDTO updated = weatherHistoryService.updateHistory(id, historyDTO);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteHistory(@PathVariable Long id) {
        weatherHistoryService.deleteHistory(id);
        return ResponseEntity.noContent().build();
    }
}