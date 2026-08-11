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
    void authenticatedMemberCanManageSharedContentAndLeaveSpace() {
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

        ResponseEntity<SpaceController.BootstrapResponse> bootstrap = restTemplate.exchange(
                "/api/bootstrap",
                HttpMethod.GET,
                new HttpEntity<>(headersWithCookie(cookieValue)),
                SpaceController.BootstrapResponse.class
        );
        assertThat(bootstrap.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(bootstrap.getBody()).isNotNull();
        assertThat(bootstrap.getBody().user().username()).isEqualTo("test_user");
        assertThat(bootstrap.getBody().space().id()).isEqualTo(spaceId);

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

        ResponseEntity<AuthController.AuthResponse> partnerRegistered = restTemplate.postForEntity(
                "/api/auth/register",
                jsonEntity(Map.of(
                        "displayName", "另一位测试用户",
                        "username", "partner_user",
                        "password", "partner-password"
                )),
                AuthController.AuthResponse.class
        );
        assertThat(partnerRegistered.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        String partnerSessionCookie = partnerRegistered.getHeaders().getFirst(HttpHeaders.SET_COOKIE);
        assertThat(partnerSessionCookie).startsWith("belong_us_session=");
        String partnerCookieValue = partnerSessionCookie.substring(0, partnerSessionCookie.indexOf(';'));

        ResponseEntity<CoupleSpaceController.SpaceSummary> partnerJoined = restTemplate.exchange(
                "/api/spaces/join",
                HttpMethod.POST,
                jsonEntityWithCookie(Map.of("inviteCode", createdSpace.getBody().inviteCode()), partnerCookieValue),
                CoupleSpaceController.SpaceSummary.class
        );
        assertThat(partnerJoined.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<SpaceController.LetterResponse> createdLetter = restTemplate.exchange(
                "/api/letters?spaceId=" + spaceId,
                HttpMethod.POST,
                jsonEntityWithCookie(Map.of("content", "想把这句话好好留下来。"), cookieValue),
                SpaceController.LetterResponse.class
        );
        assertThat(createdLetter.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(createdLetter.getBody()).isNotNull();

        ResponseEntity<SpaceController.LetterResponse> updatedLetter = restTemplate.exchange(
                "/api/letters/" + createdLetter.getBody().id() + "?spaceId=" + spaceId,
                HttpMethod.PATCH,
                jsonEntityWithCookie(Map.of("content", "这封悄悄话也可以修改。"), cookieValue),
                SpaceController.LetterResponse.class
        );
        assertThat(updatedLetter.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(updatedLetter.getBody()).isNotNull();
        assertThat(updatedLetter.getBody().content()).isEqualTo("这封悄悄话也可以修改。");

        ResponseEntity<SpaceController.LetterResponse> repliedLetter = restTemplate.exchange(
                "/api/letters/" + createdLetter.getBody().id() + "/replies?spaceId=" + spaceId,
                HttpMethod.POST,
                jsonEntityWithCookie(Map.of("content", "我有好好收到。"), partnerCookieValue),
                SpaceController.LetterResponse.class
        );
        assertThat(repliedLetter.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(repliedLetter.getBody()).isNotNull();
        Long replyId = repliedLetter.getBody().replies().getFirst().id();

        ResponseEntity<SpaceController.LetterResponse> updatedReply = restTemplate.exchange(
                "/api/letters/" + createdLetter.getBody().id() + "/replies/" + replyId + "?spaceId=" + spaceId,
                HttpMethod.PATCH,
                jsonEntityWithCookie(Map.of("content", "我已经认真读完啦。"), partnerCookieValue),
                SpaceController.LetterResponse.class
        );
        assertThat(updatedReply.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(updatedReply.getBody()).isNotNull();
        assertThat(updatedReply.getBody().replies().getFirst().content()).isEqualTo("我已经认真读完啦。");

        ResponseEntity<SpaceController.LetterResponse> replyDeleted = restTemplate.exchange(
                "/api/letters/" + createdLetter.getBody().id() + "/replies/" + replyId + "?spaceId=" + spaceId,
                HttpMethod.DELETE,
                new HttpEntity<>(headersWithCookie(partnerCookieValue)),
                SpaceController.LetterResponse.class
        );
        assertThat(replyDeleted.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(replyDeleted.getBody()).isNotNull();
        assertThat(replyDeleted.getBody().replies()).isEmpty();

        ResponseEntity<Void> letterDeleted = restTemplate.exchange(
                "/api/letters/" + createdLetter.getBody().id() + "?spaceId=" + spaceId,
                HttpMethod.DELETE,
                new HttpEntity<>(headersWithCookie(cookieValue)),
                Void.class
        );
        assertThat(letterDeleted.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        ResponseEntity<Void> memoryDeleted = restTemplate.exchange(
                "/api/memories/" + created.getBody().id() + "?spaceId=" + spaceId,
                HttpMethod.DELETE,
                new HttpEntity<>(headersWithCookie(cookieValue)),
                Void.class
        );
        assertThat(memoryDeleted.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        ResponseEntity<SpaceController.DashboardResponse> dashboard = restTemplate.exchange(
                "/api/dashboard?spaceId=" + spaceId,
                HttpMethod.GET,
                new HttpEntity<>(headersWithCookie(cookieValue)),
                SpaceController.DashboardResponse.class
        );

        assertThat(dashboard.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(dashboard.getBody()).isNotNull();
        assertThat(dashboard.getBody().space().relationshipStartedOn().toString()).isEqualTo("2026-08-01");
        assertThat(dashboard.getBody().memories()).isEmpty();
        assertThat(dashboard.getBody().letters()).isEmpty();

        ResponseEntity<Void> leftSpace = restTemplate.exchange(
                "/api/spaces/" + spaceId + "/members/me",
                HttpMethod.DELETE,
                new HttpEntity<>(headersWithCookie(cookieValue)),
                Void.class
        );
        assertThat(leftSpace.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        ResponseEntity<SpaceController.DashboardResponse> dashboardAfterLeaving = restTemplate.exchange(
                "/api/dashboard?spaceId=" + spaceId,
                HttpMethod.GET,
                new HttpEntity<>(headersWithCookie(cookieValue)),
                SpaceController.DashboardResponse.class
        );
        assertThat(dashboardAfterLeaving.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        ResponseEntity<CoupleSpaceController.SpaceSummary> joinedAgain = restTemplate.exchange(
                "/api/spaces/join",
                HttpMethod.POST,
                jsonEntityWithCookie(Map.of("inviteCode", createdSpace.getBody().inviteCode()), cookieValue),
                CoupleSpaceController.SpaceSummary.class
        );
        assertThat(joinedAgain.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(joinedAgain.getBody()).isNotNull();
        assertThat(joinedAgain.getBody().id()).isEqualTo(spaceId);

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
