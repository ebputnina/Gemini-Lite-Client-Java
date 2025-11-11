import java.io.ByteArrayInputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

import gemini_lite.protocol.FileSystemRequestHandler;
import gemini_lite.protocol.Reply;
import gemini_lite.protocol.Request;

public class RequestHandlerTests {

    @Test
    public void fileSystemHandler_happyPath_returns20() throws Exception {
        final String wire = "gemini-lite://localhost/hello" + "\r\n"; // create a simple request (with CRLF ending ;) 
        final var in = new ByteArrayInputStream(wire.getBytes()); // wraps it into InputStream, so we can parse

        final Request req = Request.parse(in); // parses

        final FileSystemRequestHandler h = new FileSystemRequestHandler(); // create handler
        final Reply r = h.handle(req); // calls the handler to get a Reply

        assertEquals(20, r.getStatus(), "expected status 20 (success)");
        assertEquals("text/plain", r.getMessage(), "expected meta to be text/plain");
    }
}
