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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class LocationsService {

    private final LocationsRepository locationsRepository;
    private final RestTemplate restTemplate;

    @Value("${openweather.api.key}")
    private String apiKey;

    // Reverse Geocoding API URL
    private static final String REVERSE_GEOCODING_URL =
            "https://api.openweathermap.org/geo/1.0/reverse?lat=%s&lon=%s&limit=1&appid=%s";

    // Ngưỡng khoảng cách để coi là "gần" (0.1 độ ≈ 11km)
    private static final double DISTANCE_THRESHOLD = 0.1;

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
     * TÌM HOẶC TẠO LOCATION TỪ GPS COORDINATES
     *
     * Logic:
     * 1. Kiểm tra xem đã có location nào gần tọa độ này chưa (trong bán kính ~11km)
     * 2. Nếu có: Trả về location đó
     * 3. Nếu không:
     *    - Gọi OpenWeather Reverse Geocoding API để lấy tên thành phố
     *    - Tạo location mới
     *    - Lưu vào database
     *    - Trả về location mới
     */
    @Transactional
    public LocationDTO findOrCreateByCoordinates(CoordinatesRequest request) {
        BigDecimal latitude = request.getLatitude();
        BigDecimal longitude = request.getLongitude();

        log.info("🔍 Tìm hoặc tạo location từ tọa độ: {}, {}", latitude, longitude);

        // Bước 1: Tìm location gần nhất trong database
        Optional<Locations> nearbyLocation = findNearbyLocation(latitude, longitude);

        if (nearbyLocation.isPresent()) {
            Locations existing = nearbyLocation.get();
            log.info("✅ Tìm thấy location gần: {} (ID: {})", existing.getCityName(), existing.getId());
            return convertToDTO(existing);
        }

        // Bước 2: Không tìm thấy -> Gọi Reverse Geocoding API
        log.info("🌐 Đang gọi OpenWeather Reverse Geocoding API...");

        try {
            String url = String.format(REVERSE_GEOCODING_URL, latitude, longitude, apiKey);
            String response = restTemplate.getForObject(url, String.class);

            if (response == null || response.equals("[]")) {
                throw new RuntimeException("Không thể xác định địa điểm từ tọa độ này");
            }

            // Parse JSON response
            JSONArray jsonArray = new JSONArray(response);
            if (jsonArray.length() == 0) {
                throw new RuntimeException("Không tìm thấy địa điểm phù hợp");
            }

            JSONObject locationData = jsonArray.getJSONObject(0);

            // Extract data
            String cityName = locationData.optString("name", "Unknown Location");
            String country = locationData.optString("country", "Unknown");

            // Local names (nếu có tên tiếng Việt)
            String localName = cityName;
            if (locationData.has("local_names")) {
                JSONObject localNames = locationData.getJSONObject("local_names");
                if (localNames.has("vi")) {
                    localName = localNames.getString("vi");
                }
            }

            BigDecimal lat = BigDecimal.valueOf(locationData.optDouble("lat", latitude.doubleValue()));
            BigDecimal lon = BigDecimal.valueOf(locationData.optDouble("lon", longitude.doubleValue()));

            // Bước 3: Tạo location mới
            Locations newLocation = Locations.builder()
                    .latitude(lat)
                    .longitude(lon)
                    .cityName(localName)  // Dùng tên địa phương nếu có
                    .country(country)
                    .countryCode(locationData.optString("country", "XX"))
                    .build();

            Locations savedLocation = locationsRepository.save(newLocation);
            log.info("✅ Đã tạo location mới: {} (ID: {})", savedLocation.getCityName(), savedLocation.getId());

            return convertToDTO(savedLocation);

        } catch (Exception e) {
            log.error("❌ Lỗi reverse geocoding: {}", e.getMessage(), e);
            throw new RuntimeException("Không thể tạo location: " + e.getMessage());
        }
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

    /**
     * Tìm location gần vị trí này trong database
     * (trong bán kính ~11km)
     */
    private Optional<Locations> findNearbyLocation(BigDecimal latitude, BigDecimal longitude) {
        List<Locations> allLocations = locationsRepository.findAll();

        return allLocations.stream()
                .filter(loc -> {
                    double distance = calculateHaversineDistance(
                            latitude.doubleValue(),
                            longitude.doubleValue(),
                            loc.getLatitude().doubleValue(),
                            loc.getLongitude().doubleValue()
                    );
                    boolean isNear = distance < DISTANCE_THRESHOLD;

                    if (isNear) {
                        log.debug("📍 Location gần: {} - Khoảng cách: {:.2f} độ",
                                loc.getCityName(),  String.format("%.2f", distance));
                    }

                    return isNear;
                })
                .findFirst();
    }



    /**
     * [OPTIONAL] Haversine formula - tính khoảng cách chính xác (km)
     * Uncomment nếu muốn dùng thay cho calculateSimpleDistance
     */
    private double calculateHaversineDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // Bán kính Trái Đất (km)

        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);

        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return R * c; // Khoảng cách tính bằng km
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