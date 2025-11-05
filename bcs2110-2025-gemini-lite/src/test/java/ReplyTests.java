import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;

import gemini_lite.protocol.ProtocolSyntaxException;
import gemini_lite.protocol.Reply;

public class ReplyTests {

    // Round-trip test for Reply headers:
    // 1) parse from a header string → check fields
    // 2) write to wire format (with CRLF) → check exact bytes
    // 3) parse back from wire bytes → check fields again
    @Test
    public void parseFormatRoundTrip() throws Exception {

        // Reply class parses the message inot a Reply object
        Reply r = Reply.fromHeaderLine("20 text/plain"); // an imaginary successful (20) reply (text/plain is the type of the message )
        // double checks that the object has what we expect
        assertEquals(20, r.getStatus()); 
        assertEquals("text/plain", r.getMessage());

        final ByteArrayOutputStream out = new ByteArrayOutputStream(); // make like a fake network cable in memory; anything we "send" gies into this buffer as bytes
        r.writeTo(out);
        final String wire = out.toString("UTF-8"); // turn the bytes into a string 
        assertEquals("20 text/plain\r\n", wire); // check that it ends as expected

        Reply r2 = Reply.parse(new ByteArrayInputStream(wire.getBytes(StandardCharsets.UTF_8))); // parse the bytes back into a ByteArrayInputSteam
        assertEquals(20, r2.getStatus());
        assertEquals("text/plain", r2.getMessage());
        // if after the rouns tript the fields are the same, we're good - it was conistent and lossless
    }

    @Test
    public void rejectOutOfRangeLow() {
        // Status codes below 10 are invalid; creating a Reply should throw a ProtocolSyntaxException
        assertThrows(ProtocolSyntaxException.class, () -> Reply.fromHeaderLine("9 nothing"));
    }

    @Test
    public void rejectOutOfRangeHigh() {
        // Status codes above 59 are invalid; creating a Reply should throw a ProtocolSyntaxException
        assertThrows(ProtocolSyntaxException.class, () -> Reply.fromHeaderLine("60 something"));
    }
}
