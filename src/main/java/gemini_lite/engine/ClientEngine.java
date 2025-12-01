package gemini_lite.engine;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import gemini_lite.protocol.ProtocolSyntaxException;
import gemini_lite.protocol.Reply;
import gemini_lite.protocol.Wire;

/**
 * Main engine for making Gemini-Lite protocol requests as a client.
 * This class handles connecting to a server (optionally through a proxy),
 * formatting a request, sending it, and retrieving the response.
 */
public class ClientEngine {
    private static final int DEFAULT_PORT = 1958;
    private String proxyHost = null; // proxy is optional so we can either have null as no proxy or default port when the proxy is there
    private int proxyPort = DEFAULT_PORT;
    private InputStream lastReplyBody = null;

    /**
     * Sets up a proxy for requests. If not set, requests go directly to server.
     * @param host Proxy server address
     * @param port Proxy server port
     */
    public void setProxy(String host, int port) {
        this.proxyHost = host;
        this.proxyPort = port;
    }

    /**
     * Sends a request to the provided Gemini-Lite URI and returns the server's reply.
     * @param uri the destination URI
     * @return Reply object parsed from server's response
     * @throws Exception in case something goes wrong (and a lot can go wrong (╥﹏╥) )
     */
    public Reply sendRequest(URI uri) throws Exception {
        if (!"gemini-lite".equalsIgnoreCase(uri.getScheme())) { // enforces tyhat the scheme is gemini-lite
            throw new IllegalArgumentException("Unsupported or missing scheme: " + uri.getScheme());
        }

        final String host = uri.getHost();
        if (host == null || host.isEmpty()) { // ensures that there IS a host
            throw new IllegalArgumentException("Missing host");
        }
        final int port = (uri.getPort() == -1) ? DEFAULT_PORT : uri.getPort(); // if no port is given, use default one
        final String path = (uri.getPath() == null || uri.getPath().isEmpty()) ? "/" : uri.getPath(); // normalize to /
        final String query = (uri.getQuery() == null) ? "" : "?" + uri.getQuery(); // is there's a query, cool keep it; otherwise nothing

        final String requestLine = "gemini-lite://" + host + ((port != DEFAULT_PORT) ? (":" + port) : "") + path + query + Wire.CRLF;

        // if using a proxy, connect there; otherwise, connect directly to the server
        final String connectHost = (proxyHost != null) ? proxyHost : host;
        final int connectPort = (proxyHost != null) ? proxyPort : port;

        System.err.println("Connecting to " + connectHost + ":" + connectPort + (proxyHost != null ? " (via proxy)" : ""));

        try {
            Socket s = new Socket(connectHost, connectPort);
            s.setSoTimeout(5000); // don't hang forever, 5 sec timeout
            final InputStream in = s.getInputStream(); // input stream for sending data
            final OutputStream out = s.getOutputStream(); // output stream for receiving data

            out.write(requestLine.getBytes(StandardCharsets.UTF_8)); // send a properyl formatted request
            out.flush();
            // parsing the server's response into a Reply object for easier handling
            Reply reply = Reply.parse(in);
            this.lastReplyBody = in; // save the reply
            return reply;

        } catch (SocketException se) {
            throw new SocketException("Connection aborted while reading reply: " + se.getMessage() + ". Check that the server is still running.", se);
        }
    }

    /**
     * Helper to build a new query URI based on a base URI and some user input,
     * making sure to encode the input properly for safe URLs.
     *
     * @param baseURI The starting URI (can't be null)
     * @param input User input to append as a URL-encoded query
     * @return A new URI with the given query string
     * @throws ProtocolSyntaxException if something goes wrong building the new URI
     */
    public URI toQuery(URI baseURI, String input) throws ProtocolSyntaxException {
        if (baseURI == null) {
            throw new ProtocolSyntaxException("URI base cannot be null when creating a query.");
        }
        try{
            String encodedInput = URLEncoder.encode(input, StandardCharsets.UTF_8.toString()); // user input is encoded to use in URLs
            String newUriString = baseURI.toString();
            if (baseURI.getQuery() != null){ // if there is an existing query, remove it
                newUriString = newUriString.substring(0, newUriString.indexOf('?'));
            }
            return new URI(newUriString + "?" + encodedInput); // append the new query string and turn in into URI object
        }catch (Exception e){
            throw new ProtocolSyntaxException("Failed to make a new URI with query: " + e.getMessage());
        }
    }

    /**
     * Retrieve the InputStream containing the body of the last server response.
     * @return InputStream of the last response’s body, or null if none yet.
     */
    public InputStream getLastResponseBody() {
        return lastReplyBody;
    }
}
