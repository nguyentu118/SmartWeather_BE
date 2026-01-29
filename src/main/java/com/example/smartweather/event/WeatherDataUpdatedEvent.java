package com.example.smartweather.event;

import com.example.smartweather.model.WeatherCache;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * Event được phát ra khi có dữ liệu thời tiết mới được lưu vào cache
 */
@Getter
public class WeatherDataUpdatedEvent extends ApplicationEvent {

    private final WeatherCache weatherCache;

    public WeatherDataUpdatedEvent(Object source, WeatherCache weatherCache) {
        super(source);
        this.weatherCache = weatherCache;
    }
}