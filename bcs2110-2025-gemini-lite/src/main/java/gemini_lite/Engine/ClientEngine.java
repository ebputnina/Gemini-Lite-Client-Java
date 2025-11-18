package gemini_lite.engine;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.net.URI;
import java.nio.charset.StandardCharsets;

import gemini_lite.protocol.Reply;
import gemini_lite.protocol.Wire;

public class ClientEngine {

    private static final int DEFAULT_PORT = 1958;
    private String proxyHost = null;
    private int proxyPort = DEFAULT_PORT;

    public void setProxy(String host, int port) {
        this.proxyHost = host;
        this.proxyPort = port;
    }
    public Reply sendRequest(URI uri) throws Exception {
        if (!"gemini-lite".equalsIgnoreCase(uri.getScheme())) { // enforces tyhat the scheme is gemini-lite
            throw new IllegalArgumentException("Unsupported or missing scheme: " + uri.getScheme());
        }

        final String host = uri.getHost(); // get host, port and path
        if (host == null || host.isEmpty()) { // ensures that there IS a host
            throw new IllegalArgumentException("Missing host");
        }
        final int port = (uri.getPort() == -1) ? DEFAULT_PORT : uri.getPort(); // if no port is given, use default one
        final String path = (uri.getPath() == null || uri.getPath().isEmpty()) ? "/" : uri.getPath(); // normalize to /

        final String requestLine = "gemini-lite://" + host + ((port != DEFAULT_PORT) ? (":" + port) : "") + path + Wire.CRLF;

        // If proxy is configured, connect to the proxy host/port instead of the final host.
        final String connectHost = (proxyHost != null) ? proxyHost : host;
        final int connectPort = (proxyHost != null) ? proxyPort : port;

        System.err.println("Connecting to " + connectHost + ":" + connectPort + (proxyHost != null ? " (via proxy)" : ""));

        try (Socket s = new Socket(connectHost, connectPort)) {
            s.setSoTimeout(5000); // don't hang forever
            final InputStream in = s.getInputStream();
            final OutputStream out = s.getOutputStream();

            out.write(requestLine.getBytes(StandardCharsets.UTF_8));
            out.flush();
            System.err.println("Reply received! :) ");
            return Reply.parse(in);

        } catch (SocketException se) {
            // Windows may report: "An established connection was aborted by the software in your host machine" :(
            throw new SocketException("Connection aborted while reading reply: " + se.getMessage() + ". Check that the server is still running.", se);
        }
    }  
}
