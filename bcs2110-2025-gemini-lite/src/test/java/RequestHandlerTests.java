import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import gemini_lite.protocol.FileSystemRequestHandler;
import gemini_lite.protocol.Request;

public class RequestHandlerTests {
    private String testDir;
    @BeforeEach
    public void setup() throws Exception {
        Path tempDir = Files.createTempDirectory("gemini_test_");
        testDir = tempDir.toAbsolutePath().toString();
        Files.writeString(Path.of(testDir, "hello.txt"), "Hello, World!");
    }

    @Test
    public void fileSystemHandler_happyPath_returns20() throws Exception {
        final String wire = "gemini-lite://localhost/hello.txt" + "\r\n";
        final var in = new ByteArrayInputStream(wire.getBytes());

        final Request req = Request.parse(in);

        final FileSystemRequestHandler h = new FileSystemRequestHandler(testDir); 
        final gemini_lite.protocol.HandlerResult result = h.handle(req); 

        assertEquals(20, result.getReply().getStatus(), "expected status 20 (success)");
        assertEquals("text/plain", result.getReply().getMessage(), "expected meta to be text/plain");
        assert result.hasBody() : "expected handler to provide file body";
    }
}
