package com.plainledger.shared.json;

import java.math.BigDecimal;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.module.SimpleModule;

/**
 * The API's decimal contract (ADR-0006): every {@link BigDecimal} crosses the API as a JSON string
 * in plain notation.
 *
 * <ul>
 *   <li>Responses: {@code 1E-8} is written as {@code "0.00000001"}, {@code 40000.000} as {@code
 *       "40000"}. Trailing zeros carry no meaning for the client and are removed.
 *   <li>Requests: only strings matching {@code -?digits[.digits]} are accepted. JSON numbers are
 *       rejected because the client may already have lost precision parsing them as binary floating
 *       point; exponent notation is rejected to keep a single, unambiguous format.
 * </ul>
 *
 * <p>Registered globally, so no field can forget it.
 */
@Component
public class DecimalJsonModule extends SimpleModule {

    private static final Pattern PLAIN_DECIMAL = Pattern.compile("-?\\d+(\\.\\d+)?");

    public DecimalJsonModule() {
        super("plainledger-decimals");
        addSerializer(BigDecimal.class, new PlainStringSerializer());
        addDeserializer(BigDecimal.class, new StrictStringDeserializer());
    }

    static final class PlainStringSerializer extends ValueSerializer<BigDecimal> {

        @Override
        public void serialize(BigDecimal value, JsonGenerator generator, SerializationContext context) {
            generator.writeString(value.stripTrailingZeros().toPlainString());
        }
    }

    static final class StrictStringDeserializer extends ValueDeserializer<BigDecimal> {

        @Override
        public BigDecimal deserialize(JsonParser parser, DeserializationContext context) {
            if (parser.currentToken() != JsonToken.VALUE_STRING) {
                return context.reportInputMismatch(
                        BigDecimal.class,
                        "Decimal values must be JSON strings in plain notation, for example \"0.1\"; got %s",
                        parser.currentToken());
            }
            String text = parser.getString();
            if (!PLAIN_DECIMAL.matcher(text).matches()) {
                return context.reportInputMismatch(
                        BigDecimal.class, "\"%s\" is not a decimal in plain notation, for example \"0.1\"", text);
            }
            return new BigDecimal(text);
        }
    }
}
