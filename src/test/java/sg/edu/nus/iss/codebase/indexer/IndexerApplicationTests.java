package sg.edu.nus.iss.codebase.indexer;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(properties = {
    "spring.main.web-application-type=none",
    "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.task.TaskExecutionAutoConfiguration",
    "app.cli.enabled=false",
    "spring.main.banner-mode=off"
})
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.properties")

class IndexerApplicationTests {

	@Test
	void contextLoads() {
		// This test just ensures the Spring context loads without starting the CLI
	}

}
