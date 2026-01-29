package com.example.smartweather.model;


import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "weather_cache", indexes = {
        @Index(name = "idx_location_updated", columnList = "location_id, lastUpdated")
})
public class WeatherCache {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, precision = 5, scale = 2)
    @NotNull(message = "Nhiệt độ không được để trống")
    @DecimalMin(value = "-100.0", message = "Nhiệt độ không hợp lệ")
    @DecimalMax(value = "100.0", message = "Nhiệt độ không hợp lệ")
    private BigDecimal temperature; // Độ C

    @Column(nullable = false, precision = 5, scale = 2)
    @NotNull(message = "Độ ẩm không được để trống")
    @DecimalMin(value = "0.0", message = "Độ ẩm phải >= 0%")
    @DecimalMax(value = "100.0", message = "Độ ẩm phải <= 100%")
    private BigDecimal humidity; // %

    @Column(length = 200)
    @Size(max = 200, message = "Mô tả không quá 200 ký tự")
    private String description; // VD: "Sunny", "Cloudy"

    @Column(precision = 4, scale = 2)
    @DecimalMin(value = "0.0", message = "UV Index phải >= 0")
    @DecimalMax(value = "20.0", message = "UV Index không hợp lệ")
    private BigDecimal uvIndex;

    @Column(precision = 6, scale = 2)  // ✅ SỬA: tăng từ 5 lên 6
    @DecimalMin(value = "0.0", message = "Tốc độ gió phải >= 0")
    @DecimalMax(value = "500.0", message = "Tốc độ gió không hợp lệ")
    private BigDecimal windSpeed; // km/h

    @Column(precision = 7, scale = 2)  // ✅ SỬA: tăng từ 5 lên 7 (pressure có thể > 1013.25 hPa)
    @DecimalMin(value = "0.0", message = "Áp suất phải >= 0")
    @DecimalMax(value = "20000.0", message = "Áp suất không hợp lệ")  // ✅ SỬA: tăng max
    private BigDecimal pressure; // hPa

    @Column(precision = 6, scale = 2)  // ✅ SỬA: tăng từ 5 lên 6
    @DecimalMin(value = "0.0", message = "Tầm nhìn phải >= 0")
    @DecimalMax(value = "1000.0", message = "Tầm nhìn không hợp lệ")  // ✅ SỬA: tăng max
    private BigDecimal visibility; // km

    @Column(nullable = false)
    @NotNull(message = "Thời gian cập nhật không được để trống")
    @PastOrPresent(message = "Thời gian cập nhật không được là tương lai")
    private LocalDateTime lastUpdated;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id", nullable = false)
    private Locations location;

    // Helper method để kiểm tra cache còn hợp lệ không (VD: < 30 phút)
    @Transient
    public boolean isExpired(int minutes) {
        return lastUpdated.plusMinutes(minutes).isBefore(LocalDateTime.now());
    }
}