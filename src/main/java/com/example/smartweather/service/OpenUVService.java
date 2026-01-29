package com.example.smartweather.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import tools.jackson.databind.JsonNode;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class OpenUVService {

    private final RestTemplate restTemplate;

    // Lấy từ application.properties
    @Value("${openuv.api.key}")
    private String openUvApiKey;

    private static final String OPENUV_BASE_URL = "https://api.openuv.io/api/v1";

    /**
     * Lấy UV Index hiện tại từ OpenUV
     *
     * @param latitude Vĩ độ
     * @param longitude Kinh độ
     * @return UV Index (BigDecimal) hoặc null nếu lỗi
     */
    public BigDecimal getCurrentUVIndex(BigDecimal latitude, BigDecimal longitude) {
        try {
            String url = String.format("%s/uv?lat=%s&lng=%s",
                    OPENUV_BASE_URL,
                    latitude,
                    longitude
            );

            // OpenUV yêu cầu API key trong header, không phải query param
            HttpHeaders headers = new HttpHeaders();
            headers.set("x-access-token", openUvApiKey);
            headers.set("Content-Type", "application/json");

            HttpEntity<String> entity = new HttpEntity<>(headers);

            log.info("🌞 Gọi OpenUV API để lấy UV Index: lat={}, lon={}", latitude, longitude);

            ResponseEntity<JsonNode> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    JsonNode.class
            );

            JsonNode body = response.getBody();
            if (body != null && body.has("result")) {
                JsonNode result = body.get("result");

                // OpenUV trả về nhiều giá trị UV khác nhau:
                // - uv: Current UV index
                // - uv_max: Maximum UV index for today
                // - uv_max_time: Time when UV will be maximum

                if (result.has("uv")) {
                    double uvIndex = result.get("uv").asDouble();
                    log.info("✅ UV Index hiện tại: {}", uvIndex);
                    return new BigDecimal(uvIndex);
                }
            }

            log.warn("⚠️ Không tìm thấy UV Index trong response");
            return null;

        } catch (Exception e) {
            log.error("❌ Lỗi khi gọi OpenUV API: {}", e.getMessage());

            // Nếu lỗi là do API key, log chi tiết
            if (e.getMessage().contains("401") || e.getMessage().contains("403")) {
                log.error("❌ QUAN TRỌNG: API key không hợp lệ! Vui lòng đăng ký tại https://www.openuv.io/console");
            }

            return null;
        }
    }

    /**
     * Lấy thông tin UV chi tiết (bao gồm UV max trong ngày, safe exposure time, etc.)
     */
    public UVDetailResponse getUVDetails(BigDecimal latitude, BigDecimal longitude) {
        try {
            String url = String.format("%s/uv?lat=%s&lng=%s",
                    OPENUV_BASE_URL,
                    latitude,
                    longitude
            );

            HttpHeaders headers = new HttpHeaders();
            headers.set("x-access-token", openUvApiKey);
            headers.set("Content-Type", "application/json");

            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<JsonNode> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    JsonNode.class
            );

            JsonNode body = response.getBody();
            if (body != null && body.has("result")) {
                JsonNode result = body.get("result");

                return UVDetailResponse.builder()
                        .currentUV(result.has("uv") ? new BigDecimal(result.get("uv").asDouble()) : null)
                        .maxUV(result.has("uv_max") ? new BigDecimal(result.get("uv_max").asDouble()) : null)
                        .maxUVTime(result.has("uv_max_time") ? result.get("uv_max_time").asText() : null)
                        .ozone(result.has("ozone") ? new BigDecimal(result.get("ozone").asDouble()) : null)
                        .safeExposureTime(extractSafeExposureTime(result))
                        .build();
            }

            return null;

        } catch (Exception e) {
            log.error("❌ Lỗi khi lấy UV details: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Lấy UV forecast cho ngày mai
     */
    public BigDecimal getForecastUVIndex(BigDecimal latitude, BigDecimal longitude) {
        try {
            String url = String.format("%s/forecast?lat=%s&lng=%s",
                    OPENUV_BASE_URL,
                    latitude,
                    longitude
            );

            HttpHeaders headers = new HttpHeaders();
            headers.set("x-access-token", openUvApiKey);

            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<JsonNode> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    JsonNode.class
            );

            JsonNode body = response.getBody();
            if (body != null && body.has("result")) {
                // Lấy UV max của ngày mai
                JsonNode result = body.get("result");
                if (result.isArray() && result.size() > 0) {
                    JsonNode tomorrow = result.get(0);
                    if (tomorrow.has("uv")) {
                        return new BigDecimal(tomorrow.get("uv").asDouble());
                    }
                }
            }

            return null;

        } catch (Exception e) {
            log.error("❌ Lỗi khi lấy UV forecast: {}", e.getMessage());
            return null;
        }
    }

    // Helper method
    private String extractSafeExposureTime(JsonNode result) {
        if (result.has("safe_exposure_time")) {
            JsonNode safeTime = result.get("safe_exposure_time");
            if (safeTime.has("st1")) {
                return safeTime.get("st1").asText() + " minutes (skin type 1)";
            }
        }
        return "N/A";
    }

    // DTO cho UV details
    @lombok.Data
    @lombok.Builder
    public static class UVDetailResponse {
        private BigDecimal currentUV;
        private BigDecimal maxUV;
        private String maxUVTime;
        private BigDecimal ozone;
        private String safeExposureTime;
    }
}