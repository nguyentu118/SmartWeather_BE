package com.example.smartweather.repository;

import com.example.smartweather.model.Locations;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LocationsRepository extends JpaRepository<Locations, Long> {
    Optional<Locations> findByCityNameContainingIgnoreCase(String cityName);
}
