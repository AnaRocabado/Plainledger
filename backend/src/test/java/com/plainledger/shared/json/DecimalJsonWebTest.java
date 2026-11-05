package com.plainledger.shared.json;

import static org.assertj.core.api.Assertions.assertThat;

import com.plainledger.testsupport.Verifies;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** The decimal contract as seen through the real Spring MVC message conversion. */
@WebMvcTest(DecimalJsonWebTest.EchoController.class)
@Import(DecimalJsonWebTest.EchoController.class)
class DecimalJsonWebTest {

    @Autowired
    MockMvcTester mvc;

    record Amount(BigDecimal value) {}

    @RestController
    static class EchoController {

        @GetMapping("/api/test/decimal")
        Amount sample() {
            return new Amount(new BigDecimal("1E-8"));
        }

        @PostMapping("/api/test/decimal")
        Amount echo(@RequestBody Amount amount) {
            return amount;
        }
    }

    @Test
    @Verifies("FOUND-AC-06")
    void responsesCarryDecimalsAsPlainStrings() {
        assertThat(mvc.get().uri("/api/test/decimal"))
                .hasStatusOk()
                .bodyJson()
                .isLenientlyEqualTo("{\"value\": \"0.00000001\"}");
    }

    @Test
    @Verifies("FOUND-AC-07")
    void requestsWithDecimalStringsAreAcceptedExactly() {
        assertThat(post("{\"value\": \"0.1\"}")).hasStatusOk().bodyJson().isLenientlyEqualTo("{\"value\": \"0.1\"}");
    }

    @Test
    @Verifies({"FOUND-AC-07", "FOUND-AC-05"})
    void requestsWithJsonNumbersAreRejectedAsProblemDetails() {
        assertThat(post("{\"value\": 0.1}"))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                .bodyJson()
                .isLenientlyEqualTo("{\"status\": 400}");
    }

    @Test
    @Verifies("FOUND-AC-07")
    void requestsWithExponentNotationAreRejected() {
        assertThat(post("{\"value\": \"1e-8\"}"))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
    }

    private MockMvcTester.MockMvcRequestBuilder post(String body) {
        return mvc.post()
                .uri("/api/test/decimal")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body);
    }
}
