import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;

import gemini_lite.protocol.ProtocolSyntaxException;
import gemini_lite.protocol.Request;

public class RequestTests {

    // Round-trip test for Request parsing and formatting:
    // 1) Parse a valid request line (gemini-lite://example.com/path\r\n)
    // 2) Check that scheme and host fields are correct
    // 3) Write the Request back to wire format and confirm it matches exactly

    @Test
    public void validRequestParseFormat() throws Exception {
        final String s = "gemini-lite://example.com/path\r\n"; // create a fake request line
        Request r = Request.parse(new ByteArrayInputStream(s.getBytes(StandardCharsets.UTF_8))); // pretend it's incoming network data -> then convert it to bytes
        assertEquals("gemini-lite", r.getURI().getScheme()); // check that scheme is the same as expected
        assertEquals("example.com", r.getURI().getHost()); // check that host is the same as expected

        final ByteArrayOutputStream out = new ByteArrayOutputStream(); // take the Request object and write it back to bytes (as if it was on the wire)
        r.writeTo(out);
        assertEquals("gemini-lite://example.com/path\r\n", out.toString("UTF-8")); // convert the bytes back to String
        // if it matche perfectly, the parsing and writing agree on the wire format
    }

    @Test
    public void invalidSpacesInUri() {
        final String s = "gemini-lite://example.com/with space\r\n";
        assertThrows(URISyntaxException.class, () -> Request.parse(new ByteArrayInputStream(s.getBytes(StandardCharsets.UTF_8))));
    }

    @Test
    public void invalidEmptyLine() {
        final String s = "\r\n";
        assertThrows(ProtocolSyntaxException.class, () -> Request.parse(new ByteArrayInputStream(s.getBytes(StandardCharsets.UTF_8))));
    }
}
