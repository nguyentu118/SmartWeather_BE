package com.example.smartweather.repository;

import com.example.smartweather.model.Locations;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Optional;

@Repository
public interface LocationsRepository extends JpaRepository<Locations, Long> {


    Optional<Locations> findByLatitudeAndLongitude(
            BigDecimal latitude,
            BigDecimal longitude
    );


    Optional<Locations> findByCityNameContainingIgnoreCase(String cityName);


    boolean existsByLatitudeAndLongitude(
            BigDecimal latitude,
            BigDecimal longitude
    );
}