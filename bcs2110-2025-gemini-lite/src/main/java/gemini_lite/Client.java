package gemini_lite;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.net.URI;
import java.nio.charset.StandardCharsets;

import gemini_lite.protocol.Reply;
import gemini_lite.protocol.Wire;

public class Client {
    private static final int DEFAULT_PORT = 1958; 

    public static void main(String[] args) throws Throwable {
        if (args.length == 0) {
            System.err.println("Usage: gemini_lite.Client gemini-lite://host[:port]/path");
            System.exit(2); // exit code 2 = “bad usage”
        }

        final URI uri = new URI(args[0]);
        if (!"gemini-lite".equalsIgnoreCase(uri.getScheme())) { // enforces tyhat the scheme is gemini-lite
            System.err.println("Unsupported or missing scheme: " + uri.getScheme()); 
            System.exit(2);
        }

        final String host = uri.getHost();
        if (host == null || host.isEmpty()) { // ensures that there IS a host
            System.err.println("Missing host");
            System.exit(2); // exit code 2 = “bad usage”
        }

        final int port = (uri.getPort() == -1) ? DEFAULT_PORT : uri.getPort(); // if no port is given, use default one
        final String path = (uri.getPath() == null || uri.getPath().isEmpty()) ? "/" : uri.getPath(); // normalize to /

        final StringBuilder req = new StringBuilder();
        req.append("gemini-lite://").append(host);
        if (port != DEFAULT_PORT){
            req.append(':').append(port);
        }
        req.append(path);

        final String requestLine = req + Wire.CRLF; // finishes with \r\n

        try (Socket s = new Socket(host, port)) {
            s.setSoTimeout(5000); // don't hang forever
            final InputStream in = s.getInputStream();
            final OutputStream out = s.getOutputStream();

            System.err.println("Connected to " + host + ":" + port);

            System.err.println("Sending request: " + req.toString());
            out.write(requestLine.getBytes(StandardCharsets.UTF_8));
            out.flush();
            System.err.println("Request sent, awaiting reply...");

                try {
                    final Reply rep = Reply.parse(in); 
                    System.out.println(rep.getStatus() + " " + rep.getMessage());
                } catch (SocketException se) {
                    // Windows may report: "An established connection was aborted by the software in your host machine" :(
                    System.err.println("Connection aborted while reading reply: " + se.getMessage());
                    System.err.println("Check that the server is still running.");
                    System.exit(1);
                }

        }
    }
}

// TODO: exception handling??

