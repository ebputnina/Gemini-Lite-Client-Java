package gemini_lite.protocol;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Parses the request header line into a URI and validates it's a gemini-lite:// URI.
 */
public final class Request {

    private final URI uri; // always gemini-lite:// - the parsed URI from the request

    public Request(URI uri) {
        if (uri == null) throw new NullPointerException("uri");
        this.uri = uri;
    }

    /**
     * parses a Gemini Lite request from the input stream
     * @param in the input stream containing the request header line
     * @return the parsed Request object
     */
    public static Request parse(InputStream in) throws IOException, URISyntaxException {
        String line = Wire.readHeaderLine(in);
        URI uri = new URI(line);
        if (!"gemini-lite".equalsIgnoreCase(uri.getScheme())) {
            throw new ProtocolSyntaxException("Unsupported scheme: " + uri.getScheme());
        }
        if (uri.getHost() == null || uri.getHost().isEmpty()) { // host is required for routing
            throw new ProtocolSyntaxException("Missing host");
        }
        return new Request(uri);
    }

    /**
     * gets the parsed URI -> easier usaege later
     * @return the gemini-lite URI
     */
    public URI getURI() {
        return uri;
    }
}
