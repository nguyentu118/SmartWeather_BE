package com.example.smartweather.service;

import com.example.smartweather.dto.HealthAlertDTO;
import com.example.smartweather.exception.ResourceNotFoundException;
import com.example.smartweather.model.HealthAlerts;
import com.example.smartweather.model.WeatherCache;
import com.example.smartweather.repository.HealthAlertsRepository;
import com.example.smartweather.repository.WeatherCacheRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class HealthAlertsService {

    private final HealthAlertsRepository healthAlertsRepository;
    private final WeatherCacheRepository weatherCacheRepository;

    @Transactional(readOnly = true)
    public List<HealthAlertDTO> getAllAlerts() {
        return healthAlertsRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public HealthAlertDTO getAlertById(Long id) {
        HealthAlerts alert = healthAlertsRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy cảnh báo với ID: " + id));
        return convertToDTO(alert);
    }

    @Transactional
    public HealthAlertDTO createAlert(HealthAlertDTO alertDTO) {
        HealthAlerts alert = HealthAlerts.builder()
                .conditionType(alertDTO.getConditionType())
                .thresholdMin(alertDTO.getThresholdMin())
                .thresholdMax(alertDTO.getThresholdMax())
                .recommendation(alertDTO.getRecommendation())
                .severity(alertDTO.getSeverity())
                .build();

        HealthAlerts saved = healthAlertsRepository.save(alert);
        return convertToDTO(saved);
    }

    @Transactional
    public HealthAlertDTO updateAlert(Long id, HealthAlertDTO alertDTO) {
        HealthAlerts alert = healthAlertsRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy cảnh báo với ID: " + id));

        alert.setConditionType(alertDTO.getConditionType());
        alert.setThresholdMin(alertDTO.getThresholdMin());
        alert.setThresholdMax(alertDTO.getThresholdMax());
        alert.setRecommendation(alertDTO.getRecommendation());
        alert.setSeverity(alertDTO.getSeverity());

        HealthAlerts updated = healthAlertsRepository.save(alert);
        return convertToDTO(updated);
    }

    @Transactional
    public void deleteAlert(Long id) {
        if (!healthAlertsRepository.existsById(id)) {
            throw new ResourceNotFoundException("Không tìm thấy cảnh báo với ID: " + id);
        }
        healthAlertsRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<HealthAlertDTO> checkAlertsForWeather(Long weatherCacheId) {
        WeatherCache weather = weatherCacheRepository.findById(weatherCacheId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy dữ liệu thời tiết"));

        List<HealthAlerts> allAlerts = healthAlertsRepository.findAll();
        List<HealthAlertDTO> triggeredAlerts = new ArrayList<>();

        for (HealthAlerts alert : allAlerts) {
            BigDecimal valueToCheck = getWeatherValue(weather, alert.getConditionType());

            if (valueToCheck != null && alert.isInDangerZone(valueToCheck)) {
                triggeredAlerts.add(convertToDTO(alert));
            }
        }

        return triggeredAlerts;
    }

    private BigDecimal getWeatherValue(WeatherCache weather, String conditionType) {
        return switch (conditionType) {
            case "UV_INDEX" -> weather.getUvIndex();
            case "TEMPERATURE" -> weather.getTemperature();
            case "HUMIDITY" -> weather.getHumidity();
            case "WIND_SPEED" -> weather.getWindSpeed();
            default -> null;
        };
    }

    private HealthAlertDTO convertToDTO(HealthAlerts alert) {
        return HealthAlertDTO.builder()
                .id(alert.getId())
                .conditionType(alert.getConditionType())
                .thresholdMin(alert.getThresholdMin())
                .thresholdMax(alert.getThresholdMax())
                .recommendation(alert.getRecommendation())
                .severity(alert.getSeverity())
                .build();
    }
}