package com.example.smartweather.repository;

import com.example.smartweather.model.Locations;
import com.example.smartweather.model.WeatherHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface WeatherHistoryRepository extends JpaRepository<WeatherHistory, Long> {
    /**
     * Kiểm tra đã có dữ liệu cho location và ngày cụ thể chưa
     */
    boolean existsByLocationAndLogDate(Locations location, LocalDate logDate);

    /**
     * Tìm lịch sử theo location và ngày
     */
    Optional<WeatherHistory> findByLocationAndLogDate(Locations location, LocalDate logDate);

    /**
     * Lấy tất cả lịch sử của một location, sắp xếp theo ngày giảm dần
     */
    List<WeatherHistory> findByLocationOrderByLogDateDesc(Locations location);

    /**
     * Lấy lịch sử theo location ID
     */
    @Query("SELECT wh FROM WeatherHistory wh WHERE wh.location.id = :locationId ORDER BY wh.logDate DESC")
    List<WeatherHistory> findByLocationId(@Param("locationId") Long locationId);

    /**
     * Lấy lịch sử trong khoảng thời gian
     */
    @Query("SELECT wh FROM WeatherHistory wh WHERE wh.location = :location " +
            "AND wh.logDate BETWEEN :startDate AND :endDate ORDER BY wh.logDate DESC")
    List<WeatherHistory> findByLocationAndDateRange(
            @Param("location") Locations location,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    /**
     * Lấy N ngày gần nhất của một location
     */
    @Query("SELECT wh FROM WeatherHistory wh WHERE wh.location = :location " +
            "ORDER BY wh.logDate DESC LIMIT :limit")
    List<WeatherHistory> findRecentByLocation(
            @Param("location") Locations location,
            @Param("limit") int limit
    );

    /**
     * Tìm các ngày có biến động nhiệt độ cao (> 15°C)
     */
    @Query("SELECT wh FROM WeatherHistory wh WHERE wh.location = :location " +
            "AND (wh.maxTemperature - wh.minTemperature) > 15 " +
            "ORDER BY wh.logDate DESC")
    List<WeatherHistory> findHighVariationDays(@Param("location") Locations location);

    /**
     * Xóa dữ liệu cũ hơn N ngày (để dọn dẹp database)
     */
    @Query("DELETE FROM WeatherHistory wh WHERE wh.logDate < :cutoffDate")
    void deleteOlderThan(@Param("cutoffDate") LocalDate cutoffDate);
}
