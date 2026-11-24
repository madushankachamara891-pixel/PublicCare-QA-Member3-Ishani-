package lk.publiccare.backend.bdd;

import io.cucumber.spring.CucumberContextConfiguration;
import lk.publiccare.backend.PublicCareBackendApplication;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Starts the real PublicCare application (controllers, security, JPA, services) once for all
 * scenarios, on an in-memory H2 database. No PostgreSQL and no real credentials are needed.
 * Flyway is disabled and Hibernate creates the schema, as in the existing ReportDeletionJpaTest.
 */
@CucumberContextConfiguration
@SpringBootTest(classes = PublicCareBackendApplication.class, properties = {
        "spring.datasource.url=jdbc:h2:mem:bdd;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false",
        "publiccare.upload.directory=target/bdd-uploads"
})
@AutoConfigureMockMvc
public class CucumberSpringConfiguration {
}
