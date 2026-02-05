package com.example.smartweather.service;

import com.example.smartweather.dto.CoordinatesRequest;
import com.example.smartweather.dto.LocationDTO;
import com.example.smartweather.exception.ResourceNotFoundException;
import com.example.smartweather.model.Locations;
import com.example.smartweather.repository.LocationsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class LocationsService {

    private final LocationsRepository locationsRepository;
    private final RestTemplate restTemplate;

    @Value("${openweather.api.key}")
    private String apiKey;

    private static final String REVERSE_GEOCODING_URL =
            "https://api.openweathermap.org/geo/1.0/reverse?lat=%s&lon=%s&limit=1&appid=%s";

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

    /**
     * ✅ OPTIMIZED: Tìm hoặc tạo Location từ GPS coordinates
     *
     * Strategy: Grid-based deduplication
     * - Snap coordinates về grid cell (precision=3 → ~111m)
     * - Check unique constraint trước khi insert
     * - Tự động tái sử dụng existing location
     *
     * Flow:
     * 1. Snap tọa độ về grid (21.028511 → 21.029)
     * 2. SELECT by grid coordinates (O(1) với index)
     * 3. Nếu có → return existing
     * 4. Nếu không → reverse geocode + insert
     * 5. Handle race condition (unique constraint violation)
     */
    @Transactional
    public LocationDTO findOrCreateByCoordinates(CoordinatesRequest request) {
        BigDecimal rawLat = request.getLatitude();
        BigDecimal rawLon = request.getLongitude();

        // Bước 1: Snap về grid cell
        BigDecimal gridLat = Locations.snapToGrid(rawLat);
        BigDecimal gridLon = Locations.snapToGrid(rawLon);

        log.info("🔍 Tìm location: raw({}, {}) → grid({}, {})",
                rawLat, rawLon, gridLat, gridLon);

        // Bước 2: Tìm location đã tồn tại trong grid cell
        // ✅ OPTIMIZATION: Direct query với indexed columns (không cần findAll())
        return locationsRepository.findByLatitudeAndLongitude(gridLat, gridLon)
                .map(existing -> {
                    log.info("✅ Tái sử dụng location: {} (ID: {})",
                            existing.getCityName(), existing.getId());
                    return convertToDTO(existing);
                })
                .orElseGet(() -> createNewLocation(gridLat, gridLon));
    }

    /**
     * Tạo location mới từ grid coordinates
     * Sử dụng reverse geocoding để lấy tên thành phố
     */
    private LocationDTO createNewLocation(BigDecimal gridLat, BigDecimal gridLon) {
        log.info("🌍 Tạo location mới từ grid: ({}, {})", gridLat, gridLon);

        try {
            // Gọi OpenWeather Reverse Geocoding API
            String url = String.format(REVERSE_GEOCODING_URL, gridLat, gridLon, apiKey);
            String response = restTemplate.getForObject(url, String.class);

            if (response == null || response.equals("[]")) {
                throw new RuntimeException("Không thể xác định địa điểm từ tọa độ này");
            }

            // Parse response
            JSONArray jsonArray = new JSONArray(response);
            if (jsonArray.length() == 0) {
                throw new RuntimeException("Không tìm thấy địa điểm phù hợp");
            }

            JSONObject locationData = jsonArray.getJSONObject(0);

            // Extract data
            String cityName = locationData.optString("name", "Unknown Location");
            String country = locationData.optString("country", "Unknown");
            String countryCode = locationData.optString("country", "XX");

            // Ưu tiên tên địa phương (local_names)
            if (locationData.has("local_names")) {
                JSONObject localNames = locationData.getJSONObject("local_names");
                if (localNames.has("vi")) {
                    cityName = localNames.getString("vi");
                }
            }

            // Tạo entity
            Locations newLocation = Locations.builder()
                    .latitude(gridLat)  // Dùng grid coordinates (đã snap)
                    .longitude(gridLon)
                    .cityName(cityName)
                    .country(country)
                    .countryCode(countryCode)
                    .build();

            try {
                Locations saved = locationsRepository.save(newLocation);
                log.info("✅ Đã tạo location: {} (ID: {})", saved.getCityName(), saved.getId());
                return convertToDTO(saved);

            } catch (DataIntegrityViolationException e) {
                // Race condition: Another thread đã tạo cùng location
                log.warn("⚠️ Unique constraint violation - location đã tồn tại, retry...");

                // Retry: query lại
                return locationsRepository.findByLatitudeAndLongitude(gridLat, gridLon)
                        .map(this::convertToDTO)
                        .orElseThrow(() -> new RuntimeException(
                                "Race condition: Không thể tạo hoặc tìm location"));
            }

        } catch (Exception e) {
            log.error("❌ Lỗi khi tạo location: {}", e.getMessage(), e);
            throw new RuntimeException("Không thể tạo location: " + e.getMessage());
        }
    }

    @Transactional
    public LocationDTO createLocation(LocationDTO locationDTO) {
        // Coordinates sẽ tự động snap trong @PrePersist hook
        Locations location = Locations.builder()
                .latitude(locationDTO.getLatitude())
                .longitude(locationDTO.getLongitude())
                .cityName(locationDTO.getCityName())
                .country(locationDTO.getCountry())
                .countryCode(locationDTO.getCountryCode())
                .build();

        try {
            Locations saved = locationsRepository.save(location);
            return convertToDTO(saved);
        } catch (DataIntegrityViolationException e) {
            // Duplicate grid coordinates
            BigDecimal gridLat = Locations.snapToGrid(locationDTO.getLatitude());
            BigDecimal gridLon = Locations.snapToGrid(locationDTO.getLongitude());

            throw new RuntimeException(String.format(
                    "Location đã tồn tại tại grid cell (%.3f, %.3f)",
                    gridLat, gridLon));
        }
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

    /**
     * Helper: Convert entity to DTO
     */
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