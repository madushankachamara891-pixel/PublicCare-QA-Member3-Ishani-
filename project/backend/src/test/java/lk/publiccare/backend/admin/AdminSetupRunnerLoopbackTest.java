package lk.publiccare.backend.admin;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

// TDD (UT-14): written before the fix. Verifies which JDBC URLs count as loopback PostgreSQL.
class AdminSetupRunnerLoopbackTest {

    @Test
    void localhostIsLoopback() {
        assertTrue(AdminSetupRunner.isLoopbackPostgres("jdbc:postgresql://localhost:5432/publiccare_db"));
    }

    @Test
    void ipv4LoopbackIsLoopback() {
        assertTrue(AdminSetupRunner.isLoopbackPostgres("jdbc:postgresql://127.0.0.1:5432/publiccare_db"));
    }

    @Test
    void ipv6LoopbackInBracketsIsLoopback() {
        assertTrue(AdminSetupRunner.isLoopbackPostgres("jdbc:postgresql://[::1]:5432/publiccare_db"));
    }

    @Test
    void remoteHostIsRejected() {
        assertFalse(AdminSetupRunner.isLoopbackPostgres("jdbc:postgresql://db.example.com:5432/publiccare_db"));
    }

    @Test
    void nonPostgresDriverIsRejected() {
        assertFalse(AdminSetupRunner.isLoopbackPostgres("jdbc:mysql://localhost:3306/publiccare_db"));
    }

    @Test
    void nonJdbcValueIsRejected() {
        assertFalse(AdminSetupRunner.isLoopbackPostgres("postgresql://localhost:5432/publiccare_db"));
    }
}
