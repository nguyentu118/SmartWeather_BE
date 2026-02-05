package com.example.smartweather.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Table(name = "locations",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_grid_coordinates",
                        columnNames = {"latitude", "longitude"}
                )
        },
        indexes = {
                @Index(name = "idx_lat_lon", columnList = "latitude, longitude"),
                @Index(name = "idx_city_country", columnList = "cityName, country")
        }
)
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Locations {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Grid-snapped latitude (scale=3 → ~111m precision)
     * Tọa độ sẽ được làm tròn để tránh duplicate
     * VD: 21.028511 → 21.029
     */
    @Column(nullable = false, precision = 10, scale = 3)
    @NotNull(message = "Latitude không được để trống")
    @DecimalMin(value = "-90.0", message = "Latitude phải >= -90")
    @DecimalMax(value = "90.0", message = "Latitude phải <= 90")
    private BigDecimal latitude;

    @Column(nullable = false, precision = 10, scale = 3)
    @NotNull(message = "Longitude không được để trống")
    @DecimalMin(value = "-180.0", message = "Longitude phải >= -180")
    @DecimalMax(value = "180.0", message = "Longitude phải <= 180")
    private BigDecimal longitude;

    @Column(nullable = false, length = 100)
    @NotBlank(message = "Tên thành phố không được để trống")
    @Size(min = 2, max = 100, message = "Tên thành phố phải từ 2-100 ký tự")
    private String cityName;

    @Column(nullable = false, length = 100)
    @NotBlank(message = "Tên quốc gia không được để trống")
    @Size(min = 2, max = 100, message = "Tên quốc gia phải từ 2-100 ký tự")
    private String country;

    @Column(length = 2)
    @Pattern(regexp = "[A-Z]{2}", message = "Mã quốc gia phải là 2 chữ cái in hoa (VD: VN, US)")
    private String countryCode;

    /**
     * Helper method: Làm tròn tọa độ về grid cell
     * Precision = 3 → ~111m accuracy
     */
    public static BigDecimal snapToGrid(BigDecimal coordinate) {
        if (coordinate == null) return null;
        return coordinate.setScale(3, RoundingMode.HALF_UP);
    }

    /**
     * Pre-persist hook: Tự động snap coordinates trước khi lưu
     */
    @PrePersist
    @PreUpdate
    protected void normalizeCoordinates() {
        this.latitude = snapToGrid(this.latitude);
        this.longitude = snapToGrid(this.longitude);
    }

    /**
     * Helper method: Tính khoảng cách Haversine (km)
     * Dùng để validate nearby locations
     */
    public double distanceTo(BigDecimal otherLat, BigDecimal otherLon) {
        if (otherLat == null || otherLon == null) {
            return Double.MAX_VALUE;
        }

        final int EARTH_RADIUS_KM = 6371;

        double lat1 = Math.toRadians(this.latitude.doubleValue());
        double lat2 = Math.toRadians(otherLat.doubleValue());
        double lon1 = Math.toRadians(this.longitude.doubleValue());
        double lon2 = Math.toRadians(otherLon.doubleValue());

        double dLat = lat2 - lat1;
        double dLon = lon2 - lon1;

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(lat1) * Math.cos(lat2)
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS_KM * c;
    }

    @Override
    public String toString() {
        return String.format("%s, %s (%.3f, %.3f)",
                cityName, country, latitude, longitude);
    }
}