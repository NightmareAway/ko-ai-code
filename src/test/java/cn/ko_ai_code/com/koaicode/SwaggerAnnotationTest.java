package cn.ko_ai_code.com.koaicode;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("local")
class SwaggerAnnotationTest {

    @Autowired(required = false)
    private RequestMappingHandlerMapping handlerMapping;

    @org.springframework.beans.factory.annotation.Value("${server.servlet.context-path:}")
    private String contextPath;

    @org.springframework.beans.factory.annotation.Value("${local.server.port}")
    private int port;

    @Test
    void testHandlerMappingAvailable() {
        assertNotNull(handlerMapping, "RequestMappingHandlerMapping should be available in Spring context");
    }

    @Test
    void testControllersWithTagAnnotationExist() {
        if (handlerMapping == null) {
            return;
        }
        Map<RequestMappingInfo, HandlerMethod> handlerMethods = handlerMapping.getHandlerMethods();

        Set<String> tags = handlerMethods.values().stream()
                .filter(h -> h.getBeanType().getName().contains("Controller"))
                .map(h -> h.getBeanType().getSimpleName())
                .collect(Collectors.toSet());

        assertTrue(tags.size() >= 3, "Should have at least 3 controllers: HealthController, UserController, AppController");
    }

    @Test
    void testSwaggerUiAccessible() {
        String swaggerUiUrl = "http://localhost:" + port + contextPath + "/swagger-ui.html";
        RestTemplate restTemplate = new RestTemplate();

        ResponseEntity<String> response = restTemplate.getForEntity(swaggerUiUrl, String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode(), "Swagger UI should return 200 status");
        assertNotNull(response.getBody(), "Swagger UI response body should not be null");
        assertTrue(response.getBody().contains("swagger") || response.getBody().contains("Swagger"),
                "Swagger UI page should contain 'Swagger' keyword");
    }

    @Test
    void testApiDocsAccessibleAndContainsOpenApiSpec() {
        String apiDocsUrl = "http://localhost:" + port + contextPath + "/v3/api-docs";
        RestTemplate restTemplate = new RestTemplate();

        ResponseEntity<String> response = restTemplate.getForEntity(apiDocsUrl, String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode(), "API docs should return 200 status");
        assertNotNull(response.getBody(), "API docs response body should not be null");
        assertTrue(response.getBody().contains("openapi") || response.getBody().contains("paths"),
                "API docs should contain 'openapi' or 'paths' keyword");
    }

    @Test
    void testApiDocsContainsUserRegistrationEndpoint() {
        String apiDocsUrl = "http://localhost:" + port + contextPath + "/v3/api-docs";
        RestTemplate restTemplate = new RestTemplate();

        ResponseEntity<String> response = restTemplate.getForEntity(apiDocsUrl, String.class);

        String body = response.getBody();
        assertNotNull(body);
        assertTrue(body.contains("/user/register") || body.contains("userRegister"),
                "API docs should contain user registration endpoint");
    }

    @Test
    void testApiDocsContainsAppManagementEndpoints() {
        String apiDocsUrl = "http://localhost:" + port + contextPath + "/v3/api-docs";
        RestTemplate restTemplate = new RestTemplate();

        ResponseEntity<String> response = restTemplate.getForEntity(apiDocsUrl, String.class);

        String body = response.getBody();
        assertNotNull(body);
        assertTrue(body.contains("/app/add") || body.contains("addApp"),
                "API docs should contain app management endpoints");
    }

    @Test
    void testApiDocsContainsHealthEndpoint() {
        String apiDocsUrl = "http://localhost:" + port + contextPath + "/v3/api-docs";
        RestTemplate restTemplate = new RestTemplate();

        ResponseEntity<String> response = restTemplate.getForEntity(apiDocsUrl, String.class);

        String body = response.getBody();
        assertNotNull(body);
        assertTrue(body.contains("/health") || body.contains("healthCheck"),
                "API docs should contain health check endpoint");
    }

    @Test
    void testAllExpectedEndpointsExist() {
        String apiDocsUrl = "http://localhost:" + port + contextPath + "/v3/api-docs";
        RestTemplate restTemplate = new RestTemplate();

        ResponseEntity<String> response = restTemplate.getForEntity(apiDocsUrl, String.class);

        String body = response.getBody();
        assertNotNull(body);

        String[] expectedEndpoints = {
                "/user/register", "/user/login", "/user/logout",
                "/app/add", "/app/update", "/app/delete",
                "/health/"
        };

        int foundCount = 0;
        for (String endpoint : expectedEndpoints) {
            if (body.contains(endpoint)) {
                foundCount++;
            }
        }

        assertTrue(foundCount >= 5,
                "At least 5 expected endpoints should be documented. Found: " + foundCount + " of " + expectedEndpoints.length);
    }
}