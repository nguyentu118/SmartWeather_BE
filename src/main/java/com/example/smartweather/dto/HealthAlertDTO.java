package com.example.smartweather.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class HealthAlertDTO {
    private Long id;

    @NotBlank(message = "Loại điều kiện không được để trống")
    @Pattern(
            regexp = "UV_INDEX|TEMPERATURE|HUMIDITY|WIND_SPEED|AIR_QUALITY",
            message = "Loại điều kiện phải là: UV_INDEX, TEMPERATURE, HUMIDITY, WIND_SPEED, AIR_QUALITY"
    )
    private String conditionType;

    @DecimalMin(value = "0.0", message = "Ngưỡng tối thiểu phải >= 0")
    private BigDecimal thresholdMin;

    @DecimalMin(value = "0.0", message = "Ngưỡng tối đa phải >= 0")
    private BigDecimal thresholdMax;

    @NotBlank(message = "Khuyến nghị không được để trống")
    @Size(min = 10, max = 500, message = "Khuyến nghị phải từ 10-500 ký tự")
    private String recommendation;

    @Pattern(regexp = "INFO|WARNING|DANGER", message = "Mức độ phải là: INFO, WARNING, DANGER")
    private String severity;
}
