package com.plainledger;

import static org.assertj.core.api.Assertions.assertThat;

import com.plainledger.testsupport.Verifies;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

/** The full application context against a real PostgreSQL reports itself healthy. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class HealthIT {

    @Autowired
    MockMvcTester mvc;

    @Test
    @Verifies("FOUND-AC-02")
    void healthIsUpIncludingTheDatabase() {
        assertThat(mvc.get().uri("/actuator/health"))
                .hasStatusOk()
                .bodyJson()
                .isLenientlyEqualTo("{\"status\": \"UP\", \"components\": {\"db\": {\"status\": \"UP\"}}}");
    }
}
