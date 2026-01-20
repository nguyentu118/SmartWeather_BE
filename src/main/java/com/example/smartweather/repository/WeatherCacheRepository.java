package com.example.smartweather.repository;

import com.example.smartweather.model.WeatherCache;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WeatherCacheRepository extends JpaRepository<WeatherCache, Long> {
}
