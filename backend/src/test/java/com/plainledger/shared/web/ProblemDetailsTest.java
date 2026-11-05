package com.plainledger.shared.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.plainledger.testsupport.Verifies;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@WebMvcTest
class ProblemDetailsTest {

    @Autowired
    MockMvcTester mvc;

    @Test
    @Verifies("FOUND-AC-05")
    void unknownApiPathIsAProblemDetails404WithoutStackTrace() {
        var response = assertThat(mvc.get().uri("/api/does-not-exist"));

        response.hasStatus(HttpStatus.NOT_FOUND).hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
        response.bodyJson().isLenientlyEqualTo("{\"status\": 404}");
        response.bodyJson().doesNotHavePath("$.trace");
        response.bodyJson().doesNotHavePath("$.exception");
        response.bodyText().doesNotContain("at com.", "at org.");
    }
}
