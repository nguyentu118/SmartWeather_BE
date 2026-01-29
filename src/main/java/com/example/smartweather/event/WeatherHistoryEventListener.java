package com.example.smartweather.event;

import com.example.smartweather.model.Locations;
import com.example.smartweather.model.WeatherCache;
import com.example.smartweather.model.WeatherHistory;
import com.example.smartweather.repository.WeatherCacheRepository;
import com.example.smartweather.repository.WeatherHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Listener lắng nghe sự kiện cập nhật dữ liệu thời tiết
 * Tự động cập nhật bảng weather_history khi có dữ liệu mới
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WeatherHistoryEventListener {

    private final WeatherHistoryRepository weatherHistoryRepository;
    private final WeatherCacheRepository weatherCacheRepository;

    /**
     * Xử lý event khi có dữ liệu thời tiết mới
     * Cập nhật hoặc tạo mới record trong weather_history cho ngày hiện tại
     */
    @EventListener
    @Async
    @Transactional
    public void handleWeatherDataUpdated(WeatherDataUpdatedEvent event) {
        WeatherCache newData = event.getWeatherCache();
        Locations location = newData.getLocation();
        LocalDate today = LocalDate.now();

        try {
            // Kiểm tra đã có record cho ngày hôm nay chưa
            var existingHistory = weatherHistoryRepository
                    .findByLocationAndLogDate(location, today);

            if (existingHistory.isPresent()) {
                // Cập nhật record hiện có với dữ liệu mới
                updateExistingHistory(existingHistory.get(), location, today);
            } else {
                // Tạo record mới cho ngày hôm nay
                createNewHistory(location, today);
            }

            log.debug("📊 Đã cập nhật lịch sử thời tiết cho {} - {}",
                    location.getCityName(), today);

        } catch (Exception e) {
            log.error("❌ Lỗi khi cập nhật lịch sử thời tiết: {}", e.getMessage(), e);
        }
    }

    /**
     * Cập nhật record history hiện có bằng cách tính lại từ tất cả cache data của ngày
     */
    private void updateExistingHistory(WeatherHistory history, Locations location, LocalDate date) {
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(23, 59, 59);

        List<WeatherCache> todayData = weatherCacheRepository
                .findByLocationAndLastUpdatedBetween(location, startOfDay, endOfDay);

        if (!todayData.isEmpty()) {
            // Tính lại các giá trị thống kê
            BigDecimal avgTemp = calculateAverage(todayData, WeatherCache::getTemperature);
            BigDecimal minTemp = findMin(todayData, WeatherCache::getTemperature);
            BigDecimal maxTemp = findMax(todayData, WeatherCache::getTemperature);
            BigDecimal avgHumidity = calculateAverage(todayData, WeatherCache::getHumidity);

            // Cập nhật
            history.setAvgTemperature(avgTemp);
            history.setMinTemperature(minTemp);
            history.setMaxTemperature(maxTemp);
            history.setAvgHumidity(avgHumidity);

            weatherHistoryRepository.save(history);
        }
    }

    /**
     * Tạo record history mới cho ngày hôm nay
     */
    private void createNewHistory(Locations location, LocalDate date) {
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(23, 59, 59);

        List<WeatherCache> todayData = weatherCacheRepository
                .findByLocationAndLastUpdatedBetween(location, startOfDay, endOfDay);

        if (!todayData.isEmpty()) {
            BigDecimal avgTemp = calculateAverage(todayData, WeatherCache::getTemperature);
            BigDecimal minTemp = findMin(todayData, WeatherCache::getTemperature);
            BigDecimal maxTemp = findMax(todayData, WeatherCache::getTemperature);
            BigDecimal avgHumidity = calculateAverage(todayData, WeatherCache::getHumidity);

            WeatherHistory newHistory = WeatherHistory.builder()
                    .location(location)
                    .logDate(date)
                    .avgTemperature(avgTemp)
                    .minTemperature(minTemp)
                    .maxTemperature(maxTemp)
                    .avgHumidity(avgHumidity)
                    .totalRainfall(BigDecimal.ZERO)
                    .build();

            weatherHistoryRepository.save(newHistory);

            log.info("✨ Đã tạo lịch sử mới cho {} - {}: Avg {}°C",
                    location.getCityName(), date, avgTemp);
        }
    }

    // Helper methods
    private BigDecimal calculateAverage(List<WeatherCache> data,
                                        java.util.function.Function<WeatherCache, BigDecimal> getter) {
        return data.stream()
                .map(getter)
                .filter(v -> v != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(data.size()), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal findMin(List<WeatherCache> data,
                               java.util.function.Function<WeatherCache, BigDecimal> getter) {
        return data.stream()
                .map(getter)
                .filter(v -> v != null)
                .min(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);
    }

    private BigDecimal findMax(List<WeatherCache> data,
                               java.util.function.Function<WeatherCache, BigDecimal> getter) {
        return data.stream()
                .map(getter)
                .filter(v -> v != null)
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);
    }
}