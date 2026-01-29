package com.example.smartweather.controller;

import com.example.smartweather.dto.LocationDTO;
import com.example.smartweather.service.LocationsService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

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
        LocationDTO dto = LocationDTO.builder().cityName("Hanoi").build();
        when(locationsService.getAllLocations()).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/locations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].cityName").value("Hanoi"));
    }

    @Test
    void createLocation_WithValidData_ShouldReturn200() throws Exception {
        LocationDTO input = LocationDTO.builder().cityName("Saigon").country("Vietnam").build();
        when(locationsService.createLocation(any(LocationDTO.class))).thenReturn(input);

        mockMvc.perform(post("/api/locations")
                        .contentType(MediaType.APPLICATION_JSON) // Giờ sẽ hết lỗi
                        .content(objectMapper.writeValueAsString(input))) // Giờ sẽ hết lỗi
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cityName").value("Saigon"));
    }
}