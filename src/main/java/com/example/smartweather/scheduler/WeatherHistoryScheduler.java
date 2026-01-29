package com.example.smartweather.scheduler;

import com.example.smartweather.model.Locations;
import com.example.smartweather.model.WeatherCache;
import com.example.smartweather.model.WeatherHistory;
import com.example.smartweather.repository.LocationsRepository;
import com.example.smartweather.repository.WeatherCacheRepository;
import com.example.smartweather.repository.WeatherHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Scheduler để tự động lưu dữ liệu thời tiết vào bảng weather_history
 *
 * Chạy mỗi ngày lúc 23:59 để tổng hợp dữ liệu của ngày hôm đó
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WeatherHistoryScheduler {

    private final WeatherCacheRepository weatherCacheRepository;
    private final WeatherHistoryRepository weatherHistoryRepository;
    private final LocationsRepository locationsRepository;

    /**
     * Chạy mỗi ngày lúc 23:59:00
     * Cron format: second minute hour day month weekday
     */
    @Scheduled(cron = "0 59 23 * * *")
    @Transactional
    public void archiveDailyWeatherData() {
        log.info("🕐 [SCHEDULER] Bắt đầu lưu lịch sử thời tiết hàng ngày...");

        LocalDate today = LocalDate.now();
        List<Locations> allLocations = locationsRepository.findAll();

        int savedCount = 0;
        int skippedCount = 0;

        for (Locations location : allLocations) {
            try {
                // Kiểm tra đã có dữ liệu cho ngày này chưa
                if (weatherHistoryRepository.existsByLocationAndLogDate(location, today)) {
                    log.debug("Đã có dữ liệu cho location {} ngày {}, bỏ qua",
                            location.getCityName(), today);
                    skippedCount++;
                    continue;
                }

                // Lấy tất cả weather cache của location trong ngày hôm nay
                LocalDateTime startOfDay = today.atStartOfDay();
                LocalDateTime endOfDay = today.atTime(23, 59, 59);

                List<WeatherCache> todayWeatherData = weatherCacheRepository
                        .findByLocationAndLastUpdatedBetween(location, startOfDay, endOfDay);

                if (todayWeatherData.isEmpty()) {
                    log.debug("Không có dữ liệu cache cho location {} ngày {}",
                            location.getCityName(), today);
                    continue;
                }

                // Tính toán các giá trị tổng hợp
                WeatherHistory history = calculateDailyStatistics(location, todayWeatherData, today);

                weatherHistoryRepository.save(history);
                savedCount++;

                log.info("✅ Đã lưu lịch sử cho {}: Avg {}°C, Min {}°C, Max {}°C",
                        location.getCityName(),
                        history.getAvgTemperature(),
                        history.getMinTemperature(),
                        history.getMaxTemperature());

            } catch (Exception e) {
                log.error("❌ Lỗi khi lưu lịch sử cho location {}: {}",
                        location.getCityName(), e.getMessage(), e);
            }
        }

        log.info("🎯 [SCHEDULER] Hoàn thành: Đã lưu {}, Bỏ qua {} location",
                savedCount, skippedCount);
    }

    /**
     * Backup job - chạy mỗi 6 giờ để đảm bảo không mất dữ liệu
     * Nếu có dữ liệu cache nhưng chưa có history thì tự động tạo
     */
    @Scheduled(cron = "0 0 */6 * * *")  // Mỗi 6 giờ
    @Transactional
    public void backupMissingHistoryData() {
        log.info("🔄 [BACKUP] Kiểm tra dữ liệu lịch sử thiếu...");

        LocalDate yesterday = LocalDate.now().minusDays(1);
        List<Locations> allLocations = locationsRepository.findAll();

        int backupCount = 0;

        for (Locations location : allLocations) {
            try {
                // Kiểm tra ngày hôm qua có dữ liệu chưa
                if (!weatherHistoryRepository.existsByLocationAndLogDate(location, yesterday)) {
                    LocalDateTime startOfDay = yesterday.atStartOfDay();
                    LocalDateTime endOfDay = yesterday.atTime(23, 59, 59);

                    List<WeatherCache> yesterdayData = weatherCacheRepository
                            .findByLocationAndLastUpdatedBetween(location, startOfDay, endOfDay);

                    if (!yesterdayData.isEmpty()) {
                        WeatherHistory history = calculateDailyStatistics(location, yesterdayData, yesterday);
                        weatherHistoryRepository.save(history);
                        backupCount++;

                        log.info("📦 Backup: Đã lưu dữ liệu thiếu cho {} ngày {}",
                                location.getCityName(), yesterday);
                    }
                }
            } catch (Exception e) {
                log.error("❌ Lỗi backup cho {}: {}", location.getCityName(), e.getMessage());
            }
        }

        if (backupCount > 0) {
            log.info("✅ [BACKUP] Đã backup {} bản ghi thiếu", backupCount);
        }
    }

    /**
     * Tính toán các giá trị thống kê từ danh sách WeatherCache
     */
    private WeatherHistory calculateDailyStatistics(
            Locations location,
            List<WeatherCache> weatherDataList,
            LocalDate date) {

        // Tính nhiệt độ trung bình, min, max
        BigDecimal avgTemp = weatherDataList.stream()
                .map(WeatherCache::getTemperature)
                .filter(t -> t != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(weatherDataList.size()), 2, RoundingMode.HALF_UP);

        BigDecimal minTemp = weatherDataList.stream()
                .map(WeatherCache::getTemperature)
                .filter(t -> t != null)
                .min(BigDecimal::compareTo)
                .orElse(avgTemp);

        BigDecimal maxTemp = weatherDataList.stream()
                .map(WeatherCache::getTemperature)
                .filter(t -> t != null)
                .max(BigDecimal::compareTo)
                .orElse(avgTemp);

        // Tính độ ẩm trung bình
        BigDecimal avgHumidity = weatherDataList.stream()
                .map(WeatherCache::getHumidity)
                .filter(h -> h != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(weatherDataList.size()), 2, RoundingMode.HALF_UP);

        // Rainfall - tạm thời để null vì API miễn phí không có
        // Nếu có dữ liệu mưa trong tương lai, có thể tổng hợp ở đây

        return WeatherHistory.builder()
                .location(location)
                .logDate(date)
                .avgTemperature(avgTemp)
                .minTemperature(minTemp)
                .maxTemperature(maxTemp)
                .avgHumidity(avgHumidity)
                .totalRainfall(BigDecimal.ZERO)  // Mặc định 0 nếu không có dữ liệu
                .build();
    }
}