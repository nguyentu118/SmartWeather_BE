package com.example.smartweather.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

@Table(name = "locations", indexes = {
        @Index(name = "idx_city_country", columnList = "cityName, country"),
        @Index(name = "idx_lat_lon", columnList = "latitude, longitude")
})
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

    @Column(nullable = false, precision = 10, scale = 7)
    @NotNull(message = "Latitude không được để trống")  // ← SỬA: @NotBlank → @NotNull (vì BigDecimal)
    @DecimalMin(value = "-90.0", message = "Latitude phải >= -90")
    @DecimalMax(value = "90.0", message = "Latitude phải <= 90")
    private BigDecimal latitude;

    @Column(nullable = false, precision = 10, scale = 7)
    @NotNull(message = "Longitude không được để trống")  // ← SỬA: @NotBlank → @NotNull
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
}