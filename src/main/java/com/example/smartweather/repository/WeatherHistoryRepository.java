package com.example.smartweather.repository;

import com.example.smartweather.model.WeatherHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WeatherHistoryRepository extends JpaRepository<WeatherHistory, Long> {
}
