package com.example.smartweather.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class LocationDTO {
    private Long id;

    @NotNull(message = "Latitude không được để trống")
    @DecimalMin(value = "-90.0", message = "Latitude phải >= -90")
    @DecimalMax(value = "90.0", message = "Latitude phải <= 90")
    private BigDecimal latitude;

    @NotNull(message = "Longitude không được để trống")
    @DecimalMin(value = "-180.0", message = "Longitude phải >= -180")
    @DecimalMax(value = "180.0", message = "Longitude phải <= 180")
    private BigDecimal longitude;

    @NotBlank(message = "Tên thành phố không được để trống")
    @Size(min = 2, max = 100, message = "Tên thành phố phải từ 2-100 ký tự")
    private String cityName;

    @NotBlank(message = "Tên quốc gia không được để trống")
    @Size(min = 2, max = 100, message = "Tên quốc gia phải từ 2-100 ký tự")
    private String country;

    @Pattern(regexp = "[A-Z]{2}", message = "Mã quốc gia phải là 2 chữ cái in hoa")
    private String countryCode;
}
