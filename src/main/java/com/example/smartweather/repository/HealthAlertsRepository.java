package com.example.smartweather.repository;

import com.example.smartweather.model.HealthAlerts;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HealthAlertsRepository extends JpaRepository<HealthAlerts, Long> {
}
