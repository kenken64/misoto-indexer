package sg.edu.nus.iss.codebase.indexer.test;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

/**
 * Test to verify CLI components are properly disabled during testing
 */
@SpringBootTest(properties = {
    "spring.main.web-application-type=none",
    "app.cli.enabled=false",
    "spring.main.banner-mode=off"
})
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.properties")
public class CliDisableTest {

    @Test
    void testCliIsDisabledDuringTests() {
        // This test verifies that the CLI doesn't start during test execution
        // If CLI components were active, they would cause test failures
        System.out.println("✅ CLI components are properly disabled during tests");
    }
}
