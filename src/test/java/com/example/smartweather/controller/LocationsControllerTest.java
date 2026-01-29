package com.example.smartweather.controller;

import com.example.smartweather.dto.LocationDTO;
import com.example.smartweather.service.LocationsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LocationsController.class)
@AutoConfigureMockMvc(addFilters = false)
class LocationsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LocationsService locationsService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void getAllLocations_ShouldReturnList() throws Exception {
        // Arrange
        List<LocationDTO> list = List.of(LocationDTO.builder().cityName("Hanoi").build());
        when(locationsService.getAllLocations()).thenReturn(list);

        // Act & Assert
        mockMvc.perform(get("/api/locations")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size()").value(1))
                .andExpect(jsonPath("$[0].cityName").value("Hanoi"));
    }

    @Test
    void createLocation_WithValidData_ShouldReturn200() throws Exception {
        // Arrange
        LocationDTO input = LocationDTO.builder().cityName("Saigon").country("Vietnam").build();
        when(locationsService.createLocation(any(LocationDTO.class))).thenReturn(input);

        // Act & Assert
        mockMvc.perform(post("/api/locations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cityName").value("Saigon"));
    }

    @Test
    void deleteLocation_ShouldReturn204() throws Exception {
        // Act & Assert
        mockMvc.perform(delete("/api/locations/1"))
                .andExpect(status().isNoContent());

        verify(locationsService, times(1)).deleteLocation(1L);
    }
}