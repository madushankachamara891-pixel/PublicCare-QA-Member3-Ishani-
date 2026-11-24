package lk.publiccare.backend.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sun.net.httpserver.HttpServer;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import lk.publiccare.backend.PublicCareBackendApplication;
import lk.publiccare.backend.report.ReportRepository;
import lk.publiccare.backend.user.PublicCareUser;
import lk.publiccare.backend.user.UserRepository;
import lk.publiccare.backend.user.UserRole;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * UI automation (Section 3.6) with Selenium and headless Chrome.
 *
 * The real backend runs on port 8080 (the API address built into the frontend) with in-memory H2.
 * The built frontend (project/dist, created by "npm run build") is served on port 5199 by a small
 * server inside this test. No frontend or application source code is changed.
 *
 * The class name ends in IT so that "mvnw test" does not pick it up. Run it with:
 *   mvnw test -Dtest=PublicCareUiIT
 * Test IDs refer to the proposed UI cases in qa/PART01_TEST_PLAN_AND_STLC.md. All data is synthetic.
 */
@SpringBootTest(classes = PublicCareBackendApplication.class, webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT,
        properties = {
                "server.port=8080",
                "PUBLICCARE_CORS_ALLOWED_ORIGIN=http://localhost:5199",
                "spring.datasource.url=jdbc:h2:mem:uitest;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
                "spring.datasource.username=sa",
                "spring.datasource.password=",
                "spring.jpa.hibernate.ddl-auto=create-drop",
                "spring.flyway.enabled=false",
                "publiccare.upload.directory=target/ui-uploads"
        })
class PublicCareUiIT {
    private static final String BASE = "http://localhost:5199";
    private static final Path DIST = Path.of("..", "dist").toAbsolutePath().normalize();
    private static final Path SCREENSHOTS = Path.of("target", "ui-screenshots");
    private static final String PASSWORD = "Synthetic-Pass-123!";

    private static HttpServer frontend;

    @Autowired UserRepository users;
    @Autowired ReportRepository reports;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired JdbcTemplate jdbc;

    private WebDriver driver;
    private WebDriverWait wait;

    @BeforeAll
    static void serveBuiltFrontend() throws Exception {
        assertTrue(Files.exists(DIST.resolve("index.html")), "Run \"npm run build\" in project/ first: " + DIST);
        frontend = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 5199), 0);
        frontend.setExecutor(Executors.newCachedThreadPool(task -> {
            Thread thread = new Thread(task, "ui-test-frontend");
            thread.setDaemon(true);
            return thread;
        }));
        frontend.createContext("/", exchange -> {
            Path file = DIST.resolve(exchange.getRequestURI().getPath().substring(1)).normalize();
            // Unknown paths return index.html, because the frontend does its own routing.
            if (!file.startsWith(DIST) || !Files.isRegularFile(file)) file = DIST.resolve("index.html");
            String name = file.getFileName().toString();
            String type = name.endsWith(".js") ? "text/javascript" : name.endsWith(".css") ? "text/css"
                    : name.endsWith(".svg") ? "image/svg+xml" : "text/html; charset=utf-8";
            byte[] body = Files.readAllBytes(file);
            exchange.getResponseHeaders().set("Content-Type", type);
            exchange.sendResponseHeaders(200, body.length);
            try (var out = exchange.getResponseBody()) {
                out.write(body);
            }
        });
        frontend.start();
    }

    @AfterAll
    static void stopFrontend() {
        if (frontend != null) frontend.stop(0);
    }

    @BeforeEach
    void openBrowser() {
        // Same H2 workaround as ReportDeletionJpaTest: PostgreSQL generates reports.reference, H2 does not.
        jdbc.execute("alter table reports alter column reference drop not null");
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless=new", "--window-size=1400,1400", "--disable-dev-shm-usage");
        driver = new ChromeDriver(options);
        wait = new WebDriverWait(driver, Duration.ofSeconds(15));
    }

    @AfterEach
    void saveScreenshotAndCloseBrowser(TestInfo info) throws Exception {
        if (driver == null) return;
        try {
            Files.createDirectories(SCREENSHOTS);
            byte[] png = ((ChromeDriver) driver).getScreenshotAs(OutputType.BYTES);
            Files.write(SCREENSHOTS.resolve(info.getTestMethod().orElseThrow().getName() + ".png"), png);
        } finally {
            driver.quit();
        }
    }

    @Test
    @DisplayName("UI-01: registration form boundaries and password mismatch")
    void registrationBoundariesAndMismatch() {
        driver.get(BASE + "/register");
        visible(By.id("register-name")).sendKeys("N".repeat(121));
        assertEquals(120, value("register-name").length(), "The name field must hold at most 120 characters.");

        String tooShort = uniqueEmail("ui-p11");
        submitRegistration(tooShort, "a".repeat(11), "a".repeat(11));
        assertTrue(validity("register-password", "tooShort"));
        assertNotRegistered(tooShort);

        for (int length : new int[] {12, 72}) {
            String email = uniqueEmail("ui-p" + length);
            submitRegistration(email, "a".repeat(length), "a".repeat(length));
            WebElement success = visible(By.cssSelector(".register-feedback[role=status]"));
            assertTrue(success.getText().startsWith("Your Citizen account has been created"), success.getText());
            wait.until(d -> value("register-email").isEmpty());
            assertTrue(users.existsByEmail(email));
        }

        String mismatch = uniqueEmail("ui-mismatch");
        submitRegistration(mismatch, PASSWORD, PASSWORD + "x");
        assertEquals("Passwords do not match.", script("return document.getElementById('register-confirm-password').validationMessage"));
        assertNotRegistered(mismatch);
    }

    @Test
    @DisplayName("UI-02: login redirects a citizen to /my-reports and an admin to /admin")
    void loginRedirectsByRole() {
        logIn(createUser(UserRole.CITIZEN), PASSWORD);
        waitForPath("/my-reports");

        visible(By.cssSelector("button[aria-label='My account']")).click();
        visible(By.xpath("//button[@role='menuitem'][contains(., 'Log out')]")).click();
        waitForPath("/");

        logIn(createUser(UserRole.ADMIN), PASSWORD);
        waitForPath("/admin");
    }

    @Test
    @DisplayName("UI-03: failed login shows the generic alert")
    void failedLoginShowsGenericAlert() {
        String registered = createUser(UserRole.CITIZEN);
        for (String email : List.of(registered, uniqueEmail("ui-unknown"))) {
            logIn(email, "Wrong-Password-123!");
            assertEquals("Invalid email or password.", visible(By.cssSelector(".login-feedback[role=alert]")).getText());
            assertEquals("/login", path());
        }
        driver.get(BASE + "/create-issue");
        visible(By.linkText("sign in"));
    }

    @Test
    @DisplayName("UI-04: signed-out user cannot submit a report")
    void signedOutUserCannotSubmitReport() {
        driver.get(BASE + "/create-issue");
        WebElement notice = visible(By.cssSelector("p.demo-notice[role=status]"));
        assertTrue(notice.getText().contains("sign in"), notice.getText());
        assertTrue(notice.findElement(By.linkText("sign in")).getDomProperty("href").endsWith("/login"));
        assertFalse(driver.findElement(By.cssSelector("button.submit-button")).isEnabled());
    }

    @Test
    @DisplayName("UI-05: required report fields and a successful submission")
    void requiredFieldsAndSuccessfulReport() {
        logIn(createUser(UserRole.CITIZEN), PASSWORD);
        waitForPath("/my-reports");
        long before = reports.count();

        for (String empty : List.of("issue-title", "issue-category", "issue-description", "issue-location")) {
            fillReport(empty);
            submitReport();
            assertTrue(validity(empty, "valueMissing"), empty + " should block the submit.");
            pause();
            assertEquals(before, reports.count(), "No report may be created when " + empty + " is empty.");
        }

        fillReport(null);
        submitReport();
        By success = By.xpath("//p[@role='status'][contains(., 'was submitted')]");
        String text = visible(success).getText();
        assertTrue(text.matches("(?s)^Report PC-\\d{8} was submitted.*"), text);
        assertEquals(before + 1, reports.count());
    }

    @Test
    @DisplayName("UI-13: admin pages are gated for a citizen session")
    void adminPagesAreGatedForCitizen() {
        logIn(createUser(UserRole.CITIZEN), PASSWORD);
        waitForPath("/my-reports");
        for (String page : List.of("/admin", "/admin/issues", "/admin/update-issue")) {
            driver.get(BASE + page);
            assertEquals("Administrator access required", visible(By.cssSelector(".auth-gate h1")).getText(), page);
        }
    }

    // ---------- helpers ----------

    private static String uniqueEmail(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8) + "@example.test";
    }

    private String createUser(UserRole role) {
        String email = uniqueEmail("ui-" + role.name().toLowerCase());
        users.saveAndFlush(new PublicCareUser("Synthetic " + role, email, passwordEncoder.encode(PASSWORD), role));
        return email;
    }

    private void logIn(String email, String password) {
        driver.get(BASE + "/login");
        visible(By.id("login-email")).sendKeys(email);
        driver.findElement(By.id("login-password")).sendKeys(password);
        driver.findElement(By.cssSelector("button.login-submit")).click();
    }

    private void submitRegistration(String email, String password, String confirmation) {
        driver.get(BASE + "/register");
        visible(By.id("register-name")).sendKeys("Synthetic Citizen");
        driver.findElement(By.id("register-email")).sendKeys(email);
        driver.findElement(By.id("register-password")).sendKeys(password);
        driver.findElement(By.id("register-confirm-password")).sendKeys(confirmation);
        driver.findElement(By.name("terms")).click();
        driver.findElement(By.cssSelector("button.register-submit")).click();
    }

    private void fillReport(String fieldToLeaveEmpty) {
        driver.get(BASE + "/create-issue");
        visible(By.id("issue-title"));
        if (!"issue-title".equals(fieldToLeaveEmpty)) type("issue-title", "Synthetic UI report");
        if (!"issue-category".equals(fieldToLeaveEmpty)) {
            new Select(driver.findElement(By.id("issue-category"))).selectByVisibleText("Drainage");
        }
        if (!"issue-description".equals(fieldToLeaveEmpty)) type("issue-description", "Synthetic description for the UI test.");
        if (!"issue-location".equals(fieldToLeaveEmpty)) type("issue-location", "Test Road");
    }

    private void submitReport() {
        wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector("button.submit-button"))).click();
    }

    private void assertNotRegistered(String email) {
        pause();
        assertFalse(users.existsByEmail(email), "No account may be created for " + email);
    }

    /** Gives a wrongly sent request time to reach the backend before checking that nothing was stored. */
    private static void pause() {
        try {
            Thread.sleep(700);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }

    private void type(String id, String text) {
        driver.findElement(By.id(id)).sendKeys(text);
    }

    private WebElement visible(By locator) {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    private void waitForPath(String expected) {
        wait.until(d -> expected.equals(path()));
    }

    private String path() {
        return URI.create(driver.getCurrentUrl()).getPath();
    }

    private String value(String id) {
        return driver.findElement(By.id(id)).getDomProperty("value");
    }

    private boolean validity(String id, String property) {
        return Boolean.TRUE.equals(script("return document.getElementById('" + id + "').validity." + property));
    }

    private Object script(String code) {
        return ((JavascriptExecutor) driver).executeScript(code);
    }
}
