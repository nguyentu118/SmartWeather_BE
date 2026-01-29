package com.example.smartweather.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CoordinatesRequest {

    @NotNull(message = "Latitude không được để trống")
    private BigDecimal latitude;

    @NotNull(message = "Longitude không được để trống")
    private BigDecimal longitude;
}