package com.example.smartweather.controller;

import com.example.smartweather.dto.LocationDTO;
import com.example.smartweather.service.LocationsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("api/locations")
@RequiredArgsConstructor
public class LocationsController {
    private final LocationsService locationsService;

    @GetMapping()
    public ResponseEntity<List<LocationDTO>> getAllLocations() {
        List<LocationDTO> locations = locationsService.getAllLocations();
        return ResponseEntity.ok(locations);
    }

    @GetMapping("/{id}")
    public ResponseEntity<LocationDTO> getLocationById(@PathVariable Long id) {
        LocationDTO location = locationsService.getLocationById(id);
        return ResponseEntity.ok(location);
    }

    @GetMapping("/search")
    public ResponseEntity<LocationDTO> searchByCity(@RequestParam String cityName) {
        LocationDTO location = locationsService.searchByCity(cityName);
        return ResponseEntity.ok(location);
    }

    @PostMapping
    public ResponseEntity<LocationDTO> createLocation(@Valid @RequestBody LocationDTO locationDTO) {
        LocationDTO created = locationsService.createLocation(locationDTO);
        return ResponseEntity.ok(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<LocationDTO> updateLocation(
            @PathVariable Long id,
            @Valid @RequestBody LocationDTO locationDTO) {
        LocationDTO updated = locationsService.updateLocation(id, locationDTO);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteLocation(@PathVariable Long id) {
        locationsService.deleteLocation(id);
        return ResponseEntity.noContent().build();
    }
}
