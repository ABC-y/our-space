package com.belongus.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SpaceControllerIntegrationTest {
    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void authenticatedMemberCanCreateAndReadMemories() {
        ResponseEntity<AuthController.AuthResponse> registered = restTemplate.postForEntity(
                "/api/auth/register",
                jsonEntity(Map.of(
                        "displayName", "测试用户",
                        "username", "test_user",
                        "password", "test-password"
                )),
                AuthController.AuthResponse.class
        );

        assertThat(registered.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        String sessionCookie = registered.getHeaders().getFirst(HttpHeaders.SET_COOKIE);
        assertThat(sessionCookie).startsWith("belong_us_session=");
        String cookieValue = sessionCookie.substring(0, sessionCookie.indexOf(';'));

        ResponseEntity<CoupleSpaceController.SpaceSummary> createdSpace = restTemplate.exchange(
                "/api/spaces",
                HttpMethod.POST,
                jsonEntityWithCookie(Map.of(
                        "name", "测试空间",
                        "relationshipStartedOn", "2026-08-10"
                ), cookieValue),
                CoupleSpaceController.SpaceSummary.class
        );

        assertThat(createdSpace.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(createdSpace.getBody()).isNotNull();
        Long spaceId = createdSpace.getBody().id();

        Map<String, Object> memoryRequest = Map.of(
                "title", "数据库连接验证",
                "content", "这条回忆由集成测试写入并读取。",
                "occurredOn", "2026-08-10"
        );

        ResponseEntity<SpaceController.MemoryResponse> created = restTemplate.exchange(
                "/api/memories?spaceId=" + spaceId,
                HttpMethod.POST,
                jsonEntityWithCookie(memoryRequest, cookieValue),
                SpaceController.MemoryResponse.class
        );

        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(created.getBody()).isNotNull();
        assertThat(created.getBody().title()).isEqualTo("数据库连接验证");

        ResponseEntity<CoupleSpaceController.SpaceSummary> updatedSpace = restTemplate.exchange(
                "/api/spaces/" + spaceId,
                HttpMethod.PATCH,
                jsonEntityWithCookie(Map.of("relationshipStartedOn", "2026-08-01"), cookieValue),
                CoupleSpaceController.SpaceSummary.class
        );
        assertThat(updatedSpace.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(updatedSpace.getBody()).isNotNull();
        assertThat(updatedSpace.getBody().relationshipStartedOn().toString()).isEqualTo("2026-08-01");

        ResponseEntity<SpaceController.MemoryResponse> updatedMemory = restTemplate.exchange(
                "/api/memories/" + created.getBody().id() + "?spaceId=" + spaceId,
                HttpMethod.PATCH,
                jsonEntityWithCookie(Map.of(
                        "title", "更新后的故事",
                        "content", "这条回忆的内容和发生日期都可以修改。",
                        "occurredOn", "2026-08-09"
                ), cookieValue),
                SpaceController.MemoryResponse.class
        );
        assertThat(updatedMemory.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(updatedMemory.getBody()).isNotNull();
        assertThat(updatedMemory.getBody().title()).isEqualTo("更新后的故事");
        assertThat(updatedMemory.getBody().occurredOn().toString()).isEqualTo("2026-08-09");

        ResponseEntity<SpaceController.DashboardResponse> dashboard = restTemplate.exchange(
                "/api/dashboard?spaceId=" + spaceId,
                HttpMethod.GET,
                new HttpEntity<>(headersWithCookie(cookieValue)),
                SpaceController.DashboardResponse.class
        );

        assertThat(dashboard.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(dashboard.getBody()).isNotNull();
        assertThat(dashboard.getBody().space().relationshipStartedOn().toString()).isEqualTo("2026-08-01");
        assertThat(dashboard.getBody().memories())
                .extracting(SpaceController.MemoryResponse::title)
                .contains("更新后的故事");

        ResponseEntity<SpaceController.DashboardResponse> unauthenticated = restTemplate.getForEntity(
                "/api/dashboard?spaceId=" + spaceId,
                SpaceController.DashboardResponse.class
        );
        assertThat(unauthenticated.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    private HttpEntity<Map<String, Object>> jsonEntity(Map<String, Object> body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(body, headers);
    }

    private HttpEntity<Map<String, Object>> jsonEntityWithCookie(Map<String, Object> body, String cookie) {
        HttpHeaders headers = headersWithCookie(cookie);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(body, headers);
    }

    private HttpHeaders headersWithCookie(String cookie) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.COOKIE, cookie);
        return headers;
    }
}
