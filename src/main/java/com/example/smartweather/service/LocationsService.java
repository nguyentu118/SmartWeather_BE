package com.example.smartweather.service;

import com.example.smartweather.dto.LocationDTO;
import com.example.smartweather.exception.ResourceNotFoundException;
import com.example.smartweather.model.Locations;
import com.example.smartweather.repository.LocationsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LocationsService {

    private final LocationsRepository locationsRepository;

    @Transactional(readOnly = true)
    public List<LocationDTO> getAllLocations() {
        return locationsRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public LocationDTO getLocationById(Long id) {
        Locations location = locationsRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy địa điểm với ID: " + id));
        return convertToDTO(location);
    }

    @Transactional(readOnly = true)
    public LocationDTO searchByCity(String cityName) {
        Locations location = locationsRepository.findByCityNameContainingIgnoreCase(cityName)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thành phố: " + cityName));
        return convertToDTO(location);
    }

    @Transactional
    public LocationDTO createLocation(LocationDTO locationDTO) {
        Locations location = Locations.builder()
                .latitude(locationDTO.getLatitude())
                .longitude(locationDTO.getLongitude())
                .cityName(locationDTO.getCityName())
                .country(locationDTO.getCountry())
                .countryCode(locationDTO.getCountryCode())
                .build();

        Locations saved = locationsRepository.save(location);
        return convertToDTO(saved);
    }

    @Transactional
    public LocationDTO updateLocation(Long id, LocationDTO locationDTO) {
        Locations location = locationsRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy địa điểm với ID: " + id));

        location.setLatitude(locationDTO.getLatitude());
        location.setLongitude(locationDTO.getLongitude());
        location.setCityName(locationDTO.getCityName());
        location.setCountry(locationDTO.getCountry());
        location.setCountryCode(locationDTO.getCountryCode());

        Locations updated = locationsRepository.save(location);
        return convertToDTO(updated);
    }

    @Transactional
    public void deleteLocation(Long id) {
        if (!locationsRepository.existsById(id)) {
            throw new ResourceNotFoundException("Không tìm thấy địa điểm với ID: " + id);
        }
        locationsRepository.deleteById(id);
    }

    private LocationDTO convertToDTO(Locations location) {
        return LocationDTO.builder()
                .id(location.getId())
                .latitude(location.getLatitude())
                .longitude(location.getLongitude())
                .cityName(location.getCityName())
                .country(location.getCountry())
                .countryCode(location.getCountryCode())
                .build();
    }
}
