package com.example.smartweather.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class WeatherDTO {
    private Long id;

    @NotNull(message = "Nhiệt độ không được để trống")
    @DecimalMin(value = "-100.0", message = "Nhiệt độ không hợp lệ")
    @DecimalMax(value = "100.0", message = "Nhiệt độ không hợp lệ")
    private BigDecimal temperature;

    @NotNull(message = "Độ ẩm không được để trống")
    @DecimalMin(value = "0.0", message = "Độ ẩm phải >= 0%")
    @DecimalMax(value = "100.0", message = "Độ ẩm phải <= 100%")
    private BigDecimal humidity;

    @Size(max = 200, message = "Mô tả không quá 200 ký tự")
    private String description;

    @DecimalMin(value = "0.0", message = "UV Index phải >= 0")
    @DecimalMax(value = "20.0", message = "UV Index không hợp lệ")
    private BigDecimal uvIndex;

    @DecimalMin(value = "0.0", message = "Tốc độ gió phải >= 0")
    @DecimalMax(value = "500.0", message = "Tốc độ gió không hợp lệ")
    private BigDecimal windSpeed;

    @DecimalMin(value = "0.0", message = "Áp suất phải >= 0")
    private BigDecimal pressure;

    @DecimalMin(value = "0.0", message = "Tầm nhìn phải >= 0")
    private BigDecimal visibility;

    @NotNull(message = "Thời gian cập nhật không được để trống")
    private LocalDateTime lastUpdated;

    @NotNull(message = "Location ID không được để trống")
    private Long locationId;

    private String cityName;
}
