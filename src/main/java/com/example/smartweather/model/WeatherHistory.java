package com.example.smartweather.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(
        name = "weather_history",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_location_date",
                        columnNames = {"location_id", "logDate"}
                )
        },
        indexes = {
                @Index(name = "idx_location_date", columnList = "location_id, logDate"),
                @Index(name = "idx_log_date", columnList = "logDate")
        }
)
public class WeatherHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    @NotNull(message = "Ngày ghi nhận không được để trống")
    @PastOrPresent(message = "Ngày ghi nhận không được là tương lai")
    private LocalDate logDate;

    @Column(nullable = false, precision = 5, scale = 2)
    @NotNull(message = "Nhiệt độ trung bình không được để trống")
    @DecimalMin(value = "-100.0", message = "Nhiệt độ không hợp lệ")
    @DecimalMax(value = "100.0", message = "Nhiệt độ không hợp lệ")
    private BigDecimal avgTemperature;

    @Column(precision = 5, scale = 2)
    @DecimalMin(value = "-100.0", message = "Nhiệt độ không hợp lệ")
    @DecimalMax(value = "100.0", message = "Nhiệt độ không hợp lệ")
    private BigDecimal maxTemperature;

    @Column(precision = 5, scale = 2)
    @DecimalMin(value = "-100.0", message = "Nhiệt độ không hợp lệ")
    @DecimalMax(value = "100.0", message = "Nhiệt độ không hợp lệ")
    private BigDecimal minTemperature;

    @Column(precision = 5, scale = 2)
    @DecimalMin(value = "0.0", message = "Độ ẩm phải >= 0")
    @DecimalMax(value = "100.0", message = "Độ ẩm phải <= 100")
    private BigDecimal avgHumidity;

    @Column(precision = 5, scale = 2)
    @DecimalMin(value = "0.0", message = "Lượng mưa phải >= 0")
    private BigDecimal totalRainfall;  // mm (thêm field hữu ích)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id", nullable = false)
    @NotNull(message = "Location không được để trống")
    private Locations location;

    // Helper method: kiểm tra nhiệt độ biến động mạnh
    @Transient
    public boolean hasHighTemperatureVariation() {
        if (maxTemperature == null || minTemperature == null) return false;
        BigDecimal diff = maxTemperature.subtract(minTemperature);
        return diff.compareTo(new BigDecimal("15")) > 0;  // Biến động > 15°C
    }
}