package com.example.smartweather.controller;

import com.example.smartweather.dto.HealthAlertDTO;
import com.example.smartweather.service.HealthAlertsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/health-alerts")
@RequiredArgsConstructor
public class HealthAlertsController {

    private final HealthAlertsService healthAlertsService;

    @GetMapping
    public ResponseEntity<List<HealthAlertDTO>> getAllAlerts() {
        List<HealthAlertDTO> alerts = healthAlertsService.getAllAlerts();
        return ResponseEntity.ok(alerts);
    }

    @GetMapping("/{id}")
    public ResponseEntity<HealthAlertDTO> getAlertById(@PathVariable Long id) {
        HealthAlertDTO alert = healthAlertsService.getAlertById(id);
        return ResponseEntity.ok(alert);
    }

    @GetMapping("/check/{weatherCacheId}")
    public ResponseEntity<List<HealthAlertDTO>> checkAlertsForWeather(@PathVariable Long weatherCacheId) {
        List<HealthAlertDTO> triggeredAlerts = healthAlertsService.checkAlertsForWeather(weatherCacheId);
        return ResponseEntity.ok(triggeredAlerts);
    }

    @PostMapping
    public ResponseEntity<HealthAlertDTO> createAlert(@Valid @RequestBody HealthAlertDTO alertDTO) {
        HealthAlertDTO created = healthAlertsService.createAlert(alertDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<HealthAlertDTO> updateAlert(
            @PathVariable Long id,
            @Valid @RequestBody HealthAlertDTO alertDTO) {
        HealthAlertDTO updated = healthAlertsService.updateAlert(id, alertDTO);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAlert(@PathVariable Long id) {
        healthAlertsService.deleteAlert(id);
        return ResponseEntity.noContent().build();
    }
}