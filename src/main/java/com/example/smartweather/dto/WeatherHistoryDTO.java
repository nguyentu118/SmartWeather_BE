package com.example.smartweather.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class WeatherHistoryDTO {
    private Long id;

    @NotNull(message = "Ngày ghi nhận không được để trống")
    @PastOrPresent(message = "Ngày ghi nhận không được là tương lai")
    private LocalDate logDate;

    @NotNull(message = "Nhiệt độ trung bình không được để trống")
    @DecimalMin(value = "-100.0", message = "Nhiệt độ không hợp lệ")
    @DecimalMax(value = "100.0", message = "Nhiệt độ không hợp lệ")
    private BigDecimal avgTemperature;

    @DecimalMin(value = "-100.0", message = "Nhiệt độ không hợp lệ")
    @DecimalMax(value = "100.0", message = "Nhiệt độ không hợp lệ")
    private BigDecimal maxTemperature;

    @DecimalMin(value = "-100.0", message = "Nhiệt độ không hợp lệ")
    @DecimalMax(value = "100.0", message = "Nhiệt độ không hợp lệ")
    private BigDecimal minTemperature;

    @DecimalMin(value = "0.0", message = "Độ ẩm phải >= 0")
    @DecimalMax(value = "100.0", message = "Độ ẩm phải <= 100")
    private BigDecimal avgHumidity;

    @DecimalMin(value = "0.0", message = "Lượng mưa phải >= 0")
    private BigDecimal totalRainfall;

    @NotNull(message = "Location ID không được để trống")
    private Long locationId;

    private String cityName;

    private Boolean hasHighVariation;
}
