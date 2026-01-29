package com.example.smartweather.service;

import com.example.smartweather.dto.WeatherHistoryDTO;
import com.example.smartweather.exception.ResourceNotFoundException;
import com.example.smartweather.model.Locations;
import com.example.smartweather.model.WeatherHistory;
import com.example.smartweather.repository.LocationsRepository;
import com.example.smartweather.repository.WeatherHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WeatherHistoryService {

    private final WeatherHistoryRepository weatherHistoryRepository;
    private final LocationsRepository locationsRepository;

    @Transactional(readOnly = true)
    public List<WeatherHistoryDTO> getAllHistory() {
        return weatherHistoryRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public WeatherHistoryDTO getHistoryById(Long id) {
        WeatherHistory history = weatherHistoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lịch sử thời tiết với ID: " + id));
        return convertToDTO(history);
    }

    @Transactional(readOnly = true)
    public List<WeatherHistoryDTO> getHistoryByLocation(Long locationId) {
        Locations location = locationsRepository.findById(locationId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy địa điểm với ID: " + locationId));

        return weatherHistoryRepository.findAll().stream()
                .filter(h -> h.getLocation().getId().equals(locationId))
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public WeatherHistoryDTO createHistory(WeatherHistoryDTO historyDTO) {
        Locations location = locationsRepository.findById(historyDTO.getLocationId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy địa điểm với ID: " + historyDTO.getLocationId()));

        WeatherHistory history = WeatherHistory.builder()
                .logDate(historyDTO.getLogDate())
                .avgTemperature(historyDTO.getAvgTemperature())
                .maxTemperature(historyDTO.getMaxTemperature())
                .minTemperature(historyDTO.getMinTemperature())
                .avgHumidity(historyDTO.getAvgHumidity())
                .totalRainfall(historyDTO.getTotalRainfall())
                .location(location)
                .build();

        WeatherHistory saved = weatherHistoryRepository.save(history);
        return convertToDTO(saved);
    }

    @Transactional
    public WeatherHistoryDTO updateHistory(Long id, WeatherHistoryDTO historyDTO) {
        WeatherHistory history = weatherHistoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lịch sử thời tiết với ID: " + id));

        history.setLogDate(historyDTO.getLogDate());
        history.setAvgTemperature(historyDTO.getAvgTemperature());
        history.setMaxTemperature(historyDTO.getMaxTemperature());
        history.setMinTemperature(historyDTO.getMinTemperature());
        history.setAvgHumidity(historyDTO.getAvgHumidity());
        history.setTotalRainfall(historyDTO.getTotalRainfall());

        WeatherHistory updated = weatherHistoryRepository.save(history);
        return convertToDTO(updated);
    }

    @Transactional
    public void deleteHistory(Long id) {
        if (!weatherHistoryRepository.existsById(id)) {
            throw new ResourceNotFoundException("Không tìm thấy lịch sử thời tiết với ID: " + id);
        }
        weatherHistoryRepository.deleteById(id);
    }

    private WeatherHistoryDTO convertToDTO(WeatherHistory history) {
        return WeatherHistoryDTO.builder()
                .id(history.getId())
                .logDate(history.getLogDate())
                .avgTemperature(history.getAvgTemperature())
                .maxTemperature(history.getMaxTemperature())
                .minTemperature(history.getMinTemperature())
                .avgHumidity(history.getAvgHumidity())
                .totalRainfall(history.getTotalRainfall())
                .locationId(history.getLocation().getId())
                .cityName(history.getLocation().getCityName())
                .hasHighVariation(history.hasHighTemperatureVariation())
                .build();
    }
}