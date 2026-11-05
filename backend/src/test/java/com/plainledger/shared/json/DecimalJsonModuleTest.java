package com.plainledger.shared.json;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.plainledger.testsupport.Verifies;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import tools.jackson.databind.exc.MismatchedInputException;
import tools.jackson.databind.json.JsonMapper;

class DecimalJsonModuleTest {

    private final JsonMapper mapper =
            JsonMapper.builder().addModule(new DecimalJsonModule()).build();

    record Amount(BigDecimal value) {}

    @ParameterizedTest(name = "{0} is written as \"{1}\"")
    @CsvSource({
        "1E-8,          0.00000001",
        "40000.000,     40000",
        "0,             0",
        "0.000,         0",
        "-12.50,        -12.5",
        "1E+3,          1000",
        "123456789012345678901.123456789012345678, 123456789012345678901.123456789012345678"
    })
    @Verifies("FOUND-AC-06")
    void writesDecimalsAsPlainStringsWithoutTrailingZeros(String value, String expected) {
        String json = mapper.writeValueAsString(new Amount(new BigDecimal(value)));

        assertThat(json).isEqualTo("{\"value\":\"" + expected + "\"}");
    }

    @Test
    @Verifies("FOUND-AC-06")
    void writesNullAsNull() {
        assertThat(mapper.writeValueAsString(new Amount(null))).isEqualTo("{\"value\":null}");
    }

    @ParameterizedTest
    @ValueSource(strings = {"0.1", "-0.1", "40000", "0.000000000000000001", "0.10"})
    @Verifies("FOUND-AC-07")
    void readsPlainDecimalStringsExactly(String value) {
        Amount amount = mapper.readValue("{\"value\":\"" + value + "\"}", Amount.class);

        // Exact: same digits and same scale as the input, no binary rounding.
        assertThat(amount.value().toPlainString()).isEqualTo(value);
    }

    @ParameterizedTest
    @ValueSource(strings = {"0.1", "1", "-3.5", "1e-8"})
    @Verifies("FOUND-AC-07")
    void rejectsJsonNumbers(String number) {
        assertThatThrownBy(() -> mapper.readValue("{\"value\":" + number + "}", Amount.class))
                .isInstanceOf(MismatchedInputException.class)
                .hasMessageContaining("must be JSON strings");
    }

    @ParameterizedTest
    @ValueSource(strings = {"1e-8", "1E+3", "abc", " 1", "1.", ".5", "+1", "1,5", ""})
    @Verifies("FOUND-AC-07")
    void rejectsStringsThatAreNotPlainDecimals(String text) {
        assertThatThrownBy(() -> mapper.readValue("{\"value\":\"" + text + "\"}", Amount.class))
                .isInstanceOf(MismatchedInputException.class)
                .hasMessageContaining("not a decimal in plain notation");
    }

    @Test
    @Verifies("FOUND-AC-07")
    void rejectsOtherJsonTypes() {
        assertThatThrownBy(() -> mapper.readValue("{\"value\":true}", Amount.class))
                .isInstanceOf(MismatchedInputException.class);
    }
}
