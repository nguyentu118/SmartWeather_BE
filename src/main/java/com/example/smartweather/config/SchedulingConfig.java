package com.example.smartweather.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Configuration để bật tính năng Scheduling trong Spring Boot
 *
 * Cần thiết để các @Scheduled annotations hoạt động
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}