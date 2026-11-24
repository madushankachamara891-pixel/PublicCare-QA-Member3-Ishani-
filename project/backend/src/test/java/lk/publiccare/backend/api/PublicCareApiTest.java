package lk.publiccare.backend.api;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import java.util.Map;
import java.util.UUID;
import lk.publiccare.backend.PublicCareBackendApplication;
import lk.publiccare.backend.user.PublicCareUser;
import lk.publiccare.backend.user.UserRepository;
import lk.publiccare.backend.user.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * API automation (Section 3.6) with REST Assured against the real application on a random port
 * (real Tomcat, real HTTP, real cookies) using an in-memory H2 database.
 * Test IDs refer to the proposed API cases in qa/PART01_REQUIREMENTS_AND_RTM.md (TASK 6-C).
 * All accounts and data are synthetic.
 */
@SpringBootTest(classes = PublicCareBackendApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.datasource.url=jdbc:h2:mem:apitest;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
                "spring.datasource.username=sa",
                "spring.datasource.password=",
                "spring.jpa.hibernate.ddl-auto=create-drop",
                "spring.flyway.enabled=false",
                "publiccare.upload.directory=target/api-uploads"
        })
class PublicCareApiTest {
    private static final String PASSWORD = "Synthetic-Pass-123!";

    @LocalServerPort int port;
    @Autowired UserRepository users;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    void setUp() {
        // PostgreSQL generates reports.reference; H2's Hibernate-created schema does not (same workaround as ReportDeletionJpaTest).
        jdbc.execute("alter table reports alter column reference drop not null");
    }

    /** A browser-like HTTP client: keeps the session cookie and sends the CSRF cookie and header. */
    private final class Client {
        String sessionId;
        String csrfHeader;
        String csrfCookie;

        RequestSpecification base() {
            RequestSpecification spec = given().baseUri("http://localhost").port(port);
            if (sessionId != null) spec.cookie("JSESSIONID", sessionId);
            return spec;
        }

        RequestSpecification withCsrf() {
            if (csrfHeader == null) {
                Response response = base().get("/api/auth/csrf");
                assertEquals(200, response.statusCode());
                csrfHeader = response.path("token");
                csrfCookie = response.cookie("XSRF-TOKEN");
                assertNotNull(csrfCookie, "XSRF-TOKEN cookie must be set");
            }
            return base().cookie("XSRF-TOKEN", csrfCookie).header("X-XSRF-TOKEN", csrfHeader);
        }

        Response register(String name, String email, String password) {
            return withCsrf().contentType(ContentType.JSON)
                    .body(Map.of("fullName", name, "email", email, "password", password))
                    .post("/api/auth/register");
        }

        Response login(String email, String password) {
            Response response = withCsrf().contentType(ContentType.JSON)
                    .body(Map.of("email", email, "password", password)).post("/api/auth/login");
            if (response.statusCode() == 200 && response.cookie("JSESSIONID") != null) {
                sessionId = response.cookie("JSESSIONID");
            }
            return response;
        }

        Response createReport(String title) {
            return withCsrf().contentType(ContentType.JSON)
                    .body(Map.of("title", title, "description", "Synthetic API test description.",
                            "category", "Drainage", "location", "Test Road"))
                    .post("/api/reports");
        }
    }

    private static String uniqueEmail(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8) + "@example.test";
    }

    private Client loggedInCitizen(String email) {
        Client client = new Client();
        assertEquals(201, client.register("Synthetic Citizen", email, PASSWORD).statusCode());
        assertEquals(200, client.login(email, PASSWORD).statusCode());
        return client;
    }

    private Client loggedInAdmin() {
        String email = uniqueEmail("admin");
        users.saveAndFlush(new PublicCareUser("Synthetic Admin", email, passwordEncoder.encode(PASSWORD), UserRole.ADMIN));
        Client client = new Client();
        assertEquals(200, client.login(email, PASSWORD).statusCode());
        return client;
    }

    @Test
    @DisplayName("API-01: CSRF endpoint returns a token and the XSRF-TOKEN cookie")
    void csrfEndpointReturnsTokenAndCookie() {
        given().baseUri("http://localhost").port(port)
                .get("/api/auth/csrf")
                .then().statusCode(200)
                .body("token", notNullValue())
                .cookie("XSRF-TOKEN", notNullValue());
    }

    @Test
    @DisplayName("API-02: registration ignores a submitted ADMIN role and never returns password data")
    void registrationForcesCitizenRoleAndHidesPassword() {
        Client client = new Client();
        String email = uniqueEmail("role");
        Response response = client.withCsrf().contentType(ContentType.JSON)
                .body(Map.of("fullName", "Synthetic User", "email", email, "password", PASSWORD, "role", "ADMIN"))
                .post("/api/auth/register");
        response.then().statusCode(201)
                .body("role", equalTo("CITIZEN"))
                .body("email", equalTo(email))
                .body("$", not(org.hamcrest.Matchers.hasKey("password")))
                .body("$", not(org.hamcrest.Matchers.hasKey("passwordHash")));
    }

    @Test
    @DisplayName("API-03 (part): password of 11 characters is rejected, 12 is accepted")
    void passwordLengthBoundary() {
        Client client = new Client();
        client.register("Synthetic User", uniqueEmail("p11"), "a".repeat(11)).then().statusCode(400);
        client.register("Synthetic User", uniqueEmail("p12"), "a".repeat(12)).then().statusCode(201);
    }

    @Test
    @DisplayName("API-04 (case): duplicate email with different letter case returns 409")
    void duplicateEmailWithDifferentCaseIsRejected() {
        String email = uniqueEmail("dup");
        Client client = new Client();
        client.register("Synthetic User", email, PASSWORD).then().statusCode(201);
        client.register("Synthetic User", email.toUpperCase(), PASSWORD).then().statusCode(409);
    }

    /**
     * Characterization test of the OBSERVED behaviour. The proposed API-04 expects 409 for a padded duplicate,
     * but the email is validated (@Email) before AuthService trims it, so surrounding spaces give 400.
     * Recorded as a finding in evidence/api-automation/README.md; the expected result needs clarification.
     */
    @Test
    @DisplayName("API-04 (padding): email with surrounding spaces is rejected with 400 before trimming (observed)")
    void emailWithSurroundingSpacesIsRejectedByValidation() {
        String email = uniqueEmail("pad");
        Client client = new Client();
        client.register("Synthetic User", email, PASSWORD).then().statusCode(201);
        client.register("Synthetic User", "  " + email + "  ", PASSWORD).then().statusCode(400);
    }

    @Test
    @DisplayName("API-08: /me returns 401 without a session and 200 with a session")
    void meRequiresSession() {
        new Client().base().get("/api/auth/me").then().statusCode(401)
                .body("error", equalTo("Authentication required."));
        Client citizen = loggedInCitizen(uniqueEmail("me"));
        citizen.base().get("/api/auth/me").then().statusCode(200).body("role", equalTo("CITIZEN"));
    }

    @Test
    @DisplayName("API-09 (one endpoint): GET /api/admin/reports is 401 anonymous, 403 citizen, 200 admin")
    void adminEndpointAuthorisationMatrix() {
        new Client().base().get("/api/admin/reports").then().statusCode(401);
        loggedInCitizen(uniqueEmail("matrix")).base().get("/api/admin/reports").then().statusCode(403);
        loggedInAdmin().base().get("/api/admin/reports").then().statusCode(200);
    }

    @Test
    @DisplayName("API-10: create report ignores a foreign ownerId; status PENDING; reference PC-########")
    void createReportUsesSessionOwner() {
        String email = uniqueEmail("owner");
        Client citizen = loggedInCitizen(email);
        Number myId = citizen.base().get("/api/auth/me").path("id");
        Response response = citizen.withCsrf().contentType(ContentType.JSON)
                .body(Map.of("title", "Synthetic report", "description", "Synthetic API test description.",
                        "category", "Drainage", "location", "Test Road", "ownerId", 999999))
                .post("/api/reports");
        response.then().statusCode(201)
                .body("status", equalTo("PENDING"))
                .body("reference", matchesPattern("^PC-\\d{8}$"));
        assertEquals(myId.longValue(), ((Number) response.path("ownerId")).longValue());
    }

    @Test
    @DisplayName("API-15 (GET): another user's report and a missing report give the same 404")
    void otherUsersReportLooksLikeMissingReport() {
        Client owner = loggedInCitizen(uniqueEmail("own"));
        Number reportId = owner.createReport("Private report").path("id");
        Client stranger = loggedInCitizen(uniqueEmail("stranger"));

        Response foreign = stranger.base().get("/api/reports/" + reportId);
        Response missing = stranger.base().get("/api/reports/987654321");
        assertEquals(404, foreign.statusCode());
        assertEquals(404, missing.statusCode());
        assertEquals(missing.asString(), foreign.asString(), "Both 404 bodies must be identical (prevents ID probing).");
        owner.base().get("/api/reports/" + reportId).then().statusCode(200)
                .body("title", containsString("Private report"));
    }
}
