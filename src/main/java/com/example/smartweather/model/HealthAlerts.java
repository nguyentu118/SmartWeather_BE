package com.example.smartweather.model;


import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "health_alerts", indexes = {
        @Index(name = "idx_condition_type", columnList = "conditionType")
})
public class HealthAlerts {

    @Id  // ← SỬA: jakarta.persistence.Id (ĐÚNG)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    @NotBlank(message = "Loại điều kiện không được để trống")
    @Pattern(
            regexp = "UV_INDEX|TEMPERATURE|HUMIDITY|WIND_SPEED|AIR_QUALITY",
            message = "Loại điều kiện phải là: UV_INDEX, TEMPERATURE, HUMIDITY, WIND_SPEED, AIR_QUALITY"
    )
    private String conditionType;

    @Column(precision = 6, scale = 2)
    @DecimalMin(value = "0.0", message = "Ngưỡng tối thiểu phải >= 0")
    private BigDecimal thresholdMin;

    @Column(precision = 6, scale = 2)
    @DecimalMin(value = "0.0", message = "Ngưỡng tối đa phải >= 0")
    private BigDecimal thresholdMax;

    @Column(nullable = false, length = 500)
    @NotBlank(message = "Khuyến nghị không được để trống")
    @Size(min = 10, max = 500, message = "Khuyến nghị phải từ 10-500 ký tự")
    private String recommendation;

    @Column(length = 20)
    @Pattern(regexp = "INFO|WARNING|DANGER", message = "Mức độ phải là: INFO, WARNING, DANGER")
    private String severity;  // Mức độ nghiêm trọng

    // OPTION 1: Nếu alert rules là GLOBAL (áp dụng cho mọi location)
    // → Không cần FK

    // OPTION 2: Nếu alert rules theo từng region/location
    // → Bỏ comment dòng dưới:
    // @ManyToOne(fetch = FetchType.LAZY)
    // @JoinColumn(name = "location_id")
    // private Locations location;

    // Helper method
    public boolean isInDangerZone(BigDecimal value) {
        if (value == null) return false;
        boolean aboveMin = thresholdMin == null || value.compareTo(thresholdMin) >= 0;
        boolean belowMax = thresholdMax == null || value.compareTo(thresholdMax) <= 0;
        return aboveMin && belowMax;
    }
}