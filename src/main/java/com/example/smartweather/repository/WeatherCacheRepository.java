package com.example.smartweather.repository;

import com.example.smartweather.model.Locations;
import com.example.smartweather.model.WeatherCache;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface WeatherCacheRepository extends JpaRepository<WeatherCache, Long> {

    /**
     * Tìm dữ liệu cache mới nhất của một location
     */
    Optional<WeatherCache> findTopByLocationOrderByLastUpdatedDesc(Locations location);

    /**
     * Lấy tất cả dữ liệu cache của một location trong khoảng thời gian
     * Dùng cho việc tổng hợp dữ liệu hàng ngày
     */
    @Query("SELECT wc FROM WeatherCache wc WHERE wc.location = :location " +
            "AND wc.lastUpdated BETWEEN :startTime AND :endTime " +
            "ORDER BY wc.lastUpdated ASC")
    List<WeatherCache> findByLocationAndLastUpdatedBetween(
            @Param("location") Locations location,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    /**
     * Lấy cache theo location ID
     */
    @Query("SELECT wc FROM WeatherCache wc WHERE wc.location.id = :locationId " +
            "ORDER BY wc.lastUpdated DESC")
    List<WeatherCache> findByLocationId(@Param("locationId") Long locationId);

    /**
     * Xóa cache cũ hơn N ngày để tiết kiệm dung lượng
     */
    @Query("DELETE FROM WeatherCache wc WHERE wc.lastUpdated < :cutoffTime")
    void deleteOlderThan(@Param("cutoffTime") LocalDateTime cutoffTime);

    /**
     * Đếm số lượng cache records của một location
     */
    long countByLocation(Locations location);
}