package com.example.smartweather.service;

import com.example.smartweather.dto.LocationDTO;
import com.example.smartweather.exception.ResourceNotFoundException;
import com.example.smartweather.model.Locations;
import com.example.smartweather.repository.LocationsRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LocationsServiceTest {

    @Mock
    private LocationsRepository locationsRepository;

    @InjectMocks
    private LocationsService locationsService;

    @Test
    void getLocationById_WhenIdExists_ShouldReturnDTO() {
        // 1. Arrange
        Long id = 1L;
        Locations location = Locations.builder().id(id).cityName("Hanoi").build();
        when(locationsRepository.findById(id)).thenReturn(Optional.of(location));

        // 2. Act
        LocationDTO result = locationsService.getLocationById(id);

        // 3. Assert
        assertNotNull(result);
        assertEquals("Hanoi", result.getCityName());
        verify(locationsRepository, times(1)).findById(id);
    }

    @Test
    void getLocationById_WhenIdNotExists_ShouldThrowException() {
        // Arrange
        Long id = 1L;
        when(locationsRepository.findById(id)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> locationsService.getLocationById(id));
    }

    @Test
    void createLocation_ShouldReturnSavedDTO() {
        // Arrange
        LocationDTO inputDTO = LocationDTO.builder().cityName("Da Nang").build();
        Locations savedLocation = Locations.builder().id(1L).cityName("Da Nang").build();
        when(locationsRepository.save(any(Locations.class))).thenReturn(savedLocation);

        // Act
        LocationDTO result = locationsService.createLocation(inputDTO);

        // Assert
        assertNotNull(result.getId());
        assertEquals("Da Nang", result.getCityName());
    }
}