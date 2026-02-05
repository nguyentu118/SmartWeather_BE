package com.example.smartweather.service;

import com.example.smartweather.dto.CoordinatesRequest;
import com.example.smartweather.dto.LocationDTO;
import com.example.smartweather.model.Locations;
import com.example.smartweather.repository.LocationsRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LocationsServiceTest {

    @Mock
    private LocationsRepository locationsRepository;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private LocationsService locationsService;

    @Test
    @DisplayName("Grid snapping: 21.028511 → 21.029")
    void testGridSnapping() {
        BigDecimal input = new BigDecimal("21.028511");
        BigDecimal expected = new BigDecimal("21.029");

        BigDecimal result = Locations.snapToGrid(input);

        assertEquals(0, expected.compareTo(result));
        assertEquals(3, result.scale());
    }

    @Test
    @DisplayName("Tái sử dụng location đã tồn tại trong grid cell")
    void testReuseExistingLocation() {
        // Given: 3 users cùng khu vực (chênh nhau vài mét)
        BigDecimal lat1 = new BigDecimal("21.028511"); // User A
        BigDecimal lat2 = new BigDecimal("21.028509"); // User B (chênh 0.22m)
        BigDecimal lat3 = new BigDecimal("21.028510"); // User C
        BigDecimal lon = new BigDecimal("105.852173");

        // Grid-snapped coordinates (cùng cell)
        BigDecimal gridLat = new BigDecimal("21.029");
        BigDecimal gridLon = new BigDecimal("105.852");

        // Mock: Location đã tồn tại
        Locations existingLocation = Locations.builder()
                .id(1L)
                .latitude(gridLat)
                .longitude(gridLon)
                .cityName("Hanoi")
                .country("Vietnam")
                .build();

        when(locationsRepository.findByLatitudeAndLongitude(gridLat, gridLon))
                .thenReturn(Optional.of(existingLocation));

        // When: 3 users request weather từ 3 tọa độ khác nhau
        CoordinatesRequest req1 = new CoordinatesRequest(lat1, lon);
        CoordinatesRequest req2 = new CoordinatesRequest(lat2, lon);
        CoordinatesRequest req3 = new CoordinatesRequest(lat3, lon);

        LocationDTO result1 = locationsService.findOrCreateByCoordinates(req1);
        LocationDTO result2 = locationsService.findOrCreateByCoordinates(req2);
        LocationDTO result3 = locationsService.findOrCreateByCoordinates(req3);

        // Then: Cả 3 đều nhận cùng location (tái sử dụng)
        assertEquals(1L, result1.getId());
        assertEquals(1L, result2.getId());
        assertEquals(1L, result3.getId());

        // Verify: Chỉ query 3 lần, KHÔNG tạo mới
        verify(locationsRepository, times(3))
                .findByLatitudeAndLongitude(gridLat, gridLon);
        verify(locationsRepository, never()).save(any());
    }

    @Test
    @DisplayName("Tạo location mới khi grid cell chưa tồn tại")
    void testCreateNewLocationWhenGridEmpty() {
        // Given
        BigDecimal rawLat = new BigDecimal("21.028511");
        BigDecimal rawLon = new BigDecimal("105.852173");
        BigDecimal gridLat = new BigDecimal("21.029");
        BigDecimal gridLon = new BigDecimal("105.852");

        // Mock: Grid cell chưa tồn tại
        when(locationsRepository.findByLatitudeAndLongitude(gridLat, gridLon))
                .thenReturn(Optional.empty());

        // Mock: OpenWeather API response
        String apiResponse = """
            [
                {
                    "name": "Hanoi",
                    "country": "VN",
                    "lat": 21.0285,
                    "lon": 105.8542
                }
            ]
            """;
        when(restTemplate.getForObject(anyString(), eq(String.class)))
                .thenReturn(apiResponse);

        // Mock: Save successful
        Locations newLocation = Locations.builder()
                .id(10L)
                .latitude(gridLat)
                .longitude(gridLon)
                .cityName("Hanoi")
                .country("VN")
                .build();
        when(locationsRepository.save(any(Locations.class)))
                .thenReturn(newLocation);

        // When
        CoordinatesRequest request = new CoordinatesRequest(rawLat, rawLon);
        LocationDTO result = locationsService.findOrCreateByCoordinates(request);

        // Then
        assertNotNull(result);
        assertEquals(10L, result.getId());
        assertEquals("Hanoi", result.getCityName());

        // Verify: Đã tạo mới
        verify(locationsRepository, times(1)).save(any(Locations.class));
    }

    @Test
    @DisplayName("Handle race condition khi 2 thread cùng tạo location")
    void testHandleRaceCondition() {
        // Given
        BigDecimal rawLat = new BigDecimal("21.028511");
        BigDecimal rawLon = new BigDecimal("105.852173");
        BigDecimal gridLat = new BigDecimal("21.029");
        BigDecimal gridLon = new BigDecimal("105.852");

        // Mock: Lần đầu không tìm thấy
        when(locationsRepository.findByLatitudeAndLongitude(gridLat, gridLon))
                .thenReturn(Optional.empty())  // Lần 1: chưa có
                .thenReturn(Optional.of(       // Lần 2 (retry): đã có
                        Locations.builder()
                                .id(99L)
                                .latitude(gridLat)
                                .longitude(gridLon)
                                .cityName("Hanoi")
                                .build()
                ));

        // Mock: API call
        String apiResponse = """
            [{"name": "Hanoi", "country": "VN"}]
            """;
        when(restTemplate.getForObject(anyString(), eq(String.class)))
                .thenReturn(apiResponse);

        // Mock: Save bị unique constraint violation (race condition)
        when(locationsRepository.save(any(Locations.class)))
                .thenThrow(new DataIntegrityViolationException("Duplicate key"));

        // When
        CoordinatesRequest request = new CoordinatesRequest(rawLat, rawLon);
        LocationDTO result = locationsService.findOrCreateByCoordinates(request);

        // Then: Retry thành công
        assertNotNull(result);
        assertEquals(99L, result.getId());

        // Verify: Query 2 lần (initial + retry)
        verify(locationsRepository, times(2))
                .findByLatitudeAndLongitude(gridLat, gridLon);
    }

    @Test
    @DisplayName("Distance calculation: Hồ Hoàn Kiếm → Hồ Tây ≈ 4.3km")
    void testDistanceCalculation() {
        // Hồ Hoàn Kiếm
        Locations hoanKiem = Locations.builder()
                .latitude(new BigDecimal("21.029"))
                .longitude(new BigDecimal("105.852"))
                .build();

        // Hồ Tây
        BigDecimal hoTayLat = new BigDecimal("21.058");
        BigDecimal hoTayLon = new BigDecimal("105.824");

        double distance = hoanKiem.distanceTo(hoTayLat, hoTayLon);

        // Expected: ~4.3km (actual measured distance)
        assertTrue(distance > 4.0 && distance < 4.7,
                "Distance should be ~4.3km, got: " + distance);
    }

    @Test
    @DisplayName("Grid precision test: Các tọa độ trong 111m cùng grid")
    void testGridPrecisionRange() {
        // Điểm gốc
        BigDecimal baseLat = new BigDecimal("21.0285");
        BigDecimal baseLon = new BigDecimal("105.8520");

        // Điểm cách 50m (nằm trong grid cell)
        BigDecimal nearLat = new BigDecimal("21.0289"); // +0.0004° ≈ 44m
        BigDecimal nearLon = new BigDecimal("105.8525"); // +0.0005° ≈ 50m

        // Điểm cách 150m (nằm ngoài grid cell)
        BigDecimal farLat = new BigDecimal("21.0300");  // +0.0015° ≈ 166m
        BigDecimal farLon = new BigDecimal("105.8520");

        // Snap về grid
        BigDecimal baseGrid = Locations.snapToGrid(baseLat);
        BigDecimal nearGrid = Locations.snapToGrid(nearLat);
        BigDecimal farGrid = Locations.snapToGrid(farLat);

        // Assertions
        assertEquals(baseGrid, nearGrid, "Điểm gần phải cùng grid cell");
        assertNotEquals(baseGrid, farGrid, "Điểm xa phải khác grid cell");
    }
}