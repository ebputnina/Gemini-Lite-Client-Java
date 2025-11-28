package gemini_lite.protocol;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;

public final class Request {

    private final URI uri; // gemini-lite://

    public Request(URI uri) {
        if (uri == null) throw new NullPointerException("uri");
        this.uri = uri;
    }

    public static Request parse(InputStream in) throws IOException, URISyntaxException {
        String line = Wire.readHeaderLine(in);
        URI uri = new URI(line);
        if (!"gemini-lite".equalsIgnoreCase(uri.getScheme())) {
            throw new ProtocolSyntaxException("Unsupported scheme: " + uri.getScheme());
        }
        if (uri.getHost() == null || uri.getHost().isEmpty()) {
            throw new ProtocolSyntaxException("Missing host");
        }
        return new Request(uri);
    }

    public URI getURI() {
        return uri;
    }
}
