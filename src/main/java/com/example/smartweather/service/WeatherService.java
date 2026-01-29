package com.example.smartweather.service;

import com.example.smartweather.dto.WeatherDTO;
import com.example.smartweather.event.WeatherDataUpdatedEvent;
import com.example.smartweather.exception.ResourceNotFoundException;
import com.example.smartweather.model.Locations;
import com.example.smartweather.model.WeatherCache;
import com.example.smartweather.repository.LocationsRepository;
import com.example.smartweather.repository.WeatherCacheRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class WeatherService {

    private final WeatherCacheRepository weatherCacheRepository;
    private final LocationsRepository locationsRepository;
    private final RestTemplate restTemplate;
    private final ApplicationEventPublisher eventPublisher;

    private final OpenUVService openUVService;

    @Value("${openweather.api.key}")
    private String apiKey;

    @Value("${openweather.api.url}")
    private String apiUrl;

    @Value("${uv.source:one_call}")  // Mặc định dùng One Call API
    private String uvSource;  // Có thể là: "one_call", "openuv", "estimate", "none"

    private static final int CACHE_EXPIRY_MINUTES = 30;

    @Transactional
    public WeatherDTO getCurrentWeather(Long locationId) {
        Locations location = locationsRepository.findById(locationId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy địa điểm với ID: " + locationId));

        // Kiểm tra cache
        Optional<WeatherCache> cachedWeather = findValidCache(location);

        if (cachedWeather.isPresent()) {
            log.info("Sử dụng dữ liệu từ cache cho location: {}", location.getCityName());
            return convertToDTO(cachedWeather.get());
        }

        // Nếu cache hết hạn, gọi OpenWeatherMap API
        log.info("Cache hết hạn, gọi OpenWeatherMap API cho location: {}", location.getCityName());
        WeatherCache newWeather = fetchWeatherFromOpenWeatherAPI(location);

        // ✅ THÊM: Lấy UV Index
        BigDecimal uvIndex = fetchUVIndex(location);
        newWeather.setUvIndex(uvIndex);

        WeatherCache saved = weatherCacheRepository.save(newWeather);

        // Phát ra event
        eventPublisher.publishEvent(new WeatherDataUpdatedEvent(this, saved));
        log.debug("📡 Đã phát ra WeatherDataUpdatedEvent cho location: {}", location.getCityName());

        return convertToDTO(saved);
    }

    @Transactional(readOnly = true)
    public List<WeatherDTO> getAllWeatherData() {
        return weatherCacheRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public void refreshWeatherCache(Long locationId) {
        Locations location = locationsRepository.findById(locationId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy địa điểm với ID: " + locationId));

        WeatherCache newWeather = fetchWeatherFromOpenWeatherAPI(location);

        // ✅ THÊM: Lấy UV Index khi refresh
        BigDecimal uvIndex = fetchUVIndex(location);
        newWeather.setUvIndex(uvIndex);

        WeatherCache saved = weatherCacheRepository.save(newWeather);

        eventPublisher.publishEvent(new WeatherDataUpdatedEvent(this, saved));
        log.debug("📡 Đã phát ra WeatherDataUpdatedEvent sau khi refresh cho location: {}", location.getCityName());
    }

    // ✅ THÊM: Method mới để fetch UV Index
    private BigDecimal fetchUVIndex(Locations location) {
        try {
            return switch (uvSource.toLowerCase()) {
                case "openuv" -> {
                    BigDecimal uv = openUVService.getCurrentUVIndex(
                            location.getLatitude(),
                            location.getLongitude()
                    );
                    yield uv != null ? uv : estimateFallbackUV(location);
                }

                case "none" -> null;  // Không lấy UV

                default -> {
                    log.warn("UV source không hợp lệ: {}. Dùng fallback.", uvSource);
                    yield estimateFallbackUV(location);
                }
            };
        } catch (Exception e) {
            log.error("Lỗi khi lấy UV Index: {}", e.getMessage());
            return estimateFallbackUV(location);
        }
    }

    private BigDecimal estimateFallbackUV(Locations location) {
        // Ước lượng đơn giản dựa trên latitude
        double lat = Math.abs(location.getLatitude().doubleValue());
        int hour = LocalDateTime.now().getHour();

        // Ban đêm
        if (hour < 6 || hour > 18) {
            return BigDecimal.ZERO;
        }

        // Ngày: UV cao ở xích đạo, thấp ở cực
        if (lat < 23.5) {
            return new BigDecimal("8.0");  // Nhiệt đới
        } else if (lat < 45) {
            return new BigDecimal("6.0");  // Cận nhiệt
        } else if (lat < 60) {
            return new BigDecimal("4.0");  // Ôn đới
        } else {
            return new BigDecimal("2.0");  // Cận cực
        }
    }

    private Optional<WeatherCache> findValidCache(Locations location) {
        return weatherCacheRepository.findAll().stream()
                .filter(cache -> cache.getLocation().getId().equals(location.getId()))
                .filter(cache -> !cache.isExpired(CACHE_EXPIRY_MINUTES))
                .findFirst();
    }

    private WeatherCache fetchWeatherFromOpenWeatherAPI(Locations location) {
        try {
            String url = String.format("%s?lat=%s&lon=%s&appid=%s&units=metric&lang=vi",
                    apiUrl,
                    location.getLatitude(),
                    location.getLongitude(),
                    apiKey
            );
            log.info("Gọi OpenWeatherMap API: {}", url.replace(apiKey, "***"));

            JsonNode response = restTemplate.getForObject(url, JsonNode.class);

            if (response == null) {
                throw new RuntimeException("Không nhận được dữ liệu từ OpenWeatherMap API");
            }
            return parseOpenWeatherResponse(response, location);
        } catch (Exception e) {
            log.error("Lỗi khi gọi OpenWeatherMap API: {}", e.getMessage());
            return createMockWeather(location);
        }
    }

    private WeatherCache parseOpenWeatherResponse(JsonNode response, Locations location) {
        WeatherCache weather = new WeatherCache();
        weather.setLocation(location);

        JsonNode main = response.get("main");
        if (main != null) {
            weather.setTemperature(new BigDecimal(main.get("temp").asDouble()));
            weather.setHumidity(new BigDecimal(main.get("humidity").asDouble()));
            weather.setPressure(new BigDecimal(main.get("pressure").asDouble()));
        }

        JsonNode weatherArray = response.get("weather");
        if (weatherArray != null && weatherArray.isArray() && weatherArray.size() > 0) {
            String description = weatherArray.get(0).get("description").asText();
            weather.setDescription(description);
        }

        JsonNode wind = response.get("wind");
        if (wind != null && wind.has("speed")) {
            double windSpeedMs = wind.get("speed").asDouble();
            weather.setWindSpeed(new BigDecimal(windSpeedMs * 3.6));
        }

        if (response.has("visibility")) {
            double visibilityM = response.get("visibility").asDouble();
            weather.setVisibility(new BigDecimal(visibilityM / 1000.0));
        }

        // UV Index sẽ được set sau khi parse
        weather.setUvIndex(null);

        weather.setLastUpdated(LocalDateTime.now());

        log.info("Đã parse dữ liệu thời tiết: {}°C, {}%, {}",
                weather.getTemperature(),
                weather.getHumidity(),
                weather.getDescription());

        return weather;
    }

    private WeatherCache createMockWeather(Locations location) {
        log.warn("Sử dụng dữ liệu giả lập cho location: {}", location.getCityName());

        WeatherCache weather = new WeatherCache();
        weather.setLocation(location);
        weather.setTemperature(new BigDecimal("25.5"));
        weather.setHumidity(new BigDecimal("65.0"));
        weather.setDescription("Partly Cloudy (Mock Data)");
        weather.setUvIndex(new BigDecimal("5.0"));
        weather.setWindSpeed(new BigDecimal("15.5"));
        weather.setPressure(new BigDecimal("1013.25"));
        weather.setVisibility(new BigDecimal("10.0"));
        weather.setLastUpdated(LocalDateTime.now());

        return weather;
    }

    private WeatherDTO convertToDTO(WeatherCache weather) {
        return WeatherDTO.builder()
                .id(weather.getId())
                .temperature(weather.getTemperature())
                .humidity(weather.getHumidity())
                .description(weather.getDescription())
                .uvIndex(weather.getUvIndex())
                .windSpeed(weather.getWindSpeed())
                .pressure(weather.getPressure())
                .visibility(weather.getVisibility())
                .lastUpdated(weather.getLastUpdated())
                .locationId(weather.getLocation().getId())
                .cityName(weather.getLocation().getCityName())
                .build();
    }
}