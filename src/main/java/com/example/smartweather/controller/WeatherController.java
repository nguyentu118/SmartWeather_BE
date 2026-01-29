package com.example.smartweather.controller;

import com.example.smartweather.dto.WeatherDTO;
import com.example.smartweather.service.WeatherService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/weather")
@RequiredArgsConstructor
public class WeatherController {

    private final WeatherService weatherService;

    @GetMapping
    public ResponseEntity<List<WeatherDTO>> getAllWeather() {
        List<WeatherDTO> weatherList = weatherService.getAllWeatherData();
        return ResponseEntity.ok(weatherList);
    }

    @GetMapping("/location/{locationId}")
    public ResponseEntity<WeatherDTO> getCurrentWeather(@PathVariable Long locationId) {
        WeatherDTO weather = weatherService.getCurrentWeather(locationId);
        return ResponseEntity.ok(weather);
    }

    @PostMapping("/refresh/{locationId}")
    public ResponseEntity<String> refreshWeatherCache(@PathVariable Long locationId) {
        weatherService.refreshWeatherCache(locationId);
        return ResponseEntity.ok("Cache đã được làm mới cho địa điểm ID: " + locationId);
    }
}