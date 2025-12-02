package gemini_lite.protocol;
import java.io.IOException;
/**
 * Thrown when a Gemini Lite request has invalid syntax.
 */
public class ProtocolSyntaxException extends IOException {
    public ProtocolSyntaxException(String message) {
        super(message);
    }
}


