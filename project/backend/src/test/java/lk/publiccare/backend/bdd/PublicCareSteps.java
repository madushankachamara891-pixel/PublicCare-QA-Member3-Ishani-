package lk.publiccare.backend.bdd;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import jakarta.servlet.http.Cookie;
import java.util.Map;
import java.util.UUID;
import lk.publiccare.backend.user.PublicCareUser;
import lk.publiccare.backend.user.UserRepository;
import lk.publiccare.backend.user.UserRole;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * Step definitions for citizen_report.feature and admin_status_update.feature.
 * Every request goes through the real Spring MVC + Spring Security stack (session, CSRF, roles).
 * All accounts and report data are synthetic.
 */
public class PublicCareSteps {
    private static final String PASSWORD = "Synthetic-Pass-123!";

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired UserRepository users;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired JdbcTemplate jdbc;

    /** One browser-like client: its own session and CSRF token. */
    private final class Actor {
        final MockHttpSession session = new MockHttpSession();
        String csrfHeaderToken;  // masked token from the JSON body, sent as the X-XSRF-TOKEN header
        String csrfCookieToken;  // raw token from the XSRF-TOKEN Set-Cookie, sent back as a cookie (like a browser)

        void loadCsrf() throws Exception {
            if (csrfHeaderToken == null) {
                MvcResult result = mvc.perform(get("/api/auth/csrf").session(session)).andReturn();
                assertEquals(200, result.getResponse().getStatus());
                csrfHeaderToken = JsonPath.read(result.getResponse().getContentAsString(), "$.token");
                Cookie cookie = result.getResponse().getCookie("XSRF-TOKEN");
                assertNotNull(cookie, "The server must set the XSRF-TOKEN cookie.");
                csrfCookieToken = cookie.getValue();
            }
        }

        MvcResult send(MockHttpServletRequestBuilder request, boolean withCsrf) throws Exception {
            request.session(session);
            if (withCsrf) {
                loadCsrf();
                request.cookie(new Cookie("XSRF-TOKEN", csrfCookieToken)).header("X-XSRF-TOKEN", csrfHeaderToken);
            }
            return mvc.perform(request).andReturn();
        }

        MvcResult postJson(String url, Map<String, ?> body) throws Exception {
            return send(post(url).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)), true);
        }

        MvcResult patchJson(String url, Map<String, ?> body) throws Exception {
            return send(patch(url).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)), true);
        }

        MvcResult getJson(String url) throws Exception {
            return send(get(url), false);
        }
    }

    private Actor visitor;
    private Actor citizen;
    private Actor admin;
    private String citizenEmail;
    private Long citizenId;
    private Long reportId;
    private MvcResult last;

    @Before
    public void prepareScenario() {
        // PostgreSQL generates reports.reference; H2's Hibernate-created schema does not (same workaround as ReportDeletionJpaTest).
        jdbc.execute("alter table reports alter column reference drop not null");
        visitor = null;
        citizen = null;
        admin = null;
        citizenEmail = null;
        citizenId = null;
        reportId = null;
        last = null;
    }

    // ---------- citizen_report.feature ----------

    @Given("a visitor registers with the name {string} and a unique email")
    public void aVisitorRegisters(String name) throws Exception {
        visitor = new Actor();
        citizenEmail = "citizen-" + UUID.randomUUID().toString().substring(0, 8) + "@example.test";
        last = visitor.postJson("/api/auth/register",
                Map.of("fullName", name, "email", citizenEmail, "password", PASSWORD));
    }

    @Given("the registration is accepted with the role {string}")
    public void registrationAccepted(String role) throws Exception {
        assertEquals(201, last.getResponse().getStatus());
        String body = last.getResponse().getContentAsString();
        assertEquals(role, JsonPath.read(body, "$.role"));
        assertFalse(body.contains("password"), "The response must never contain password data.");
    }

    @Given("the citizen logs in with the registered email and password")
    public void citizenLogsIn() throws Exception {
        citizen = new Actor();
        last = citizen.postJson("/api/auth/login", Map.of("email", citizenEmail, "password", PASSWORD));
        assertEquals(200, last.getResponse().getStatus());
        citizenId = ((Number) JsonPath.read(last.getResponse().getContentAsString(), "$.id")).longValue();
    }

    @When("the citizen submits a report titled {string} in the category {string} at {string}")
    public void citizenSubmitsReport(String title, String category, String location) throws Exception {
        last = citizen.postJson("/api/reports", reportBody(title, category, location));
    }

    @Then("the report is created")
    public void reportIsCreated() throws Exception {
        assertEquals(201, last.getResponse().getStatus());
    }

    @Then("the report status is {string}")
    public void reportStatusIs(String status) throws Exception {
        assertEquals(status, JsonPath.read(last.getResponse().getContentAsString(), "$.status"));
    }

    @Then("the report reference starts with {string}")
    public void referenceStartsWith(String prefix) throws Exception {
        String reference = JsonPath.read(last.getResponse().getContentAsString(), "$.reference");
        assertNotNull(reference);
        assertTrue(reference.startsWith(prefix), "Unexpected reference: " + reference);
    }

    @Then("the report belongs to the logged in citizen")
    public void reportBelongsToCitizen() throws Exception {
        Number ownerId = JsonPath.read(last.getResponse().getContentAsString(), "$.ownerId");
        assertEquals(citizenId, ownerId.longValue());
    }

    @Given("a visitor who is not logged in")
    public void visitorNotLoggedIn() {
        visitor = new Actor();
    }

    @When("the visitor submits a report titled {string} in the category {string} at {string}")
    public void visitorSubmitsReport(String title, String category, String location) throws Exception {
        last = visitor.postJson("/api/reports", reportBody(title, category, location));
    }

    @Then("the request is rejected because authentication is required")
    public void rejectedAuthenticationRequired() {
        assertEquals(401, last.getResponse().getStatus());
    }

    // ---------- admin_status_update.feature ----------

    @Given("a citizen has submitted a report titled {string}")
    public void citizenHasSubmittedReport(String title) throws Exception {
        aVisitorRegisters("Kamal Silva");
        registrationAccepted("CITIZEN");
        citizenLogsIn();
        citizenSubmitsReport(title, "Road damage", "Temple Road, Kandy");
        reportIsCreated();
        reportId = ((Number) JsonPath.read(last.getResponse().getContentAsString(), "$.id")).longValue();
    }

    @Given("an administrator is logged in")
    public void administratorLoggedIn() throws Exception {
        // No web route creates admins (FR-06.9), so a synthetic admin is stored directly.
        String email = "admin-" + UUID.randomUUID().toString().substring(0, 8) + "@example.test";
        users.saveAndFlush(new PublicCareUser("Synthetic Admin", email, passwordEncoder.encode(PASSWORD), UserRole.ADMIN));
        admin = new Actor();
        last = admin.postJson("/api/auth/login", Map.of("email", email, "password", PASSWORD));
        assertEquals(200, last.getResponse().getStatus());
        assertEquals("ADMIN", JsonPath.read(last.getResponse().getContentAsString(), "$.role"));
    }

    @When("the administrator sets the report status to {string} with the note {string}")
    public void adminSetsStatus(String status, String note) throws Exception {
        last = admin.patchJson("/api/admin/reports/" + reportId + "/status",
                Map.of("status", status, "adminUpdateNote", note));
    }

    @Then("the status update is accepted")
    public void statusUpdateAccepted() {
        assertEquals(200, last.getResponse().getStatus());
    }

    @Then("the citizen sees the report status {string}")
    public void citizenSeesStatus(String status) throws Exception {
        MvcResult detail = citizen.getJson("/api/reports/" + reportId);
        assertEquals(200, detail.getResponse().getStatus());
        assertEquals(status, JsonPath.read(detail.getResponse().getContentAsString(), "$.status"));
    }

    @Then("the citizen sees the administrator note {string}")
    public void citizenSeesNote(String note) throws Exception {
        MvcResult detail = citizen.getJson("/api/reports/" + reportId);
        assertEquals(note, JsonPath.read(detail.getResponse().getContentAsString(), "$.adminUpdateNote"));
    }

    @When("the citizen tries to set the report status to {string}")
    public void citizenTriesToSetStatus(String status) throws Exception {
        last = citizen.patchJson("/api/admin/reports/" + reportId + "/status",
                Map.of("status", status, "adminUpdateNote", "Citizen should not be able to do this"));
    }

    @Then("the request is rejected because access is denied")
    public void rejectedAccessDenied() {
        assertEquals(403, last.getResponse().getStatus());
    }

    @Then("the report status is still {string} for the citizen")
    public void statusStillForCitizen(String status) throws Exception {
        citizenSeesStatus(status);
    }

    private static Map<String, String> reportBody(String title, String category, String location) {
        return Map.of("title", title,
                "description", "Synthetic description written for the automated BDD scenario.",
                "category", category, "location", location);
    }
}
