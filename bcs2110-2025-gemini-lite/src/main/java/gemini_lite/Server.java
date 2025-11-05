package gemini_lite;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;

import gemini_lite.protocol.ProtocolSyntaxException;
import gemini_lite.protocol.Reply;
import gemini_lite.protocol.Request;
import gemini_lite.protocol.Wire;


public class Server {
    private final int port;

    public Server(int port) {
        this.port = port;
    }

    public static void main(String[] args) throws Exception {
        final int port = (args.length > 0) ? Integer.parseInt(args[0]) : 1958; // if the user passed a port, use it, if not, default to 1958
        new Server(port).run();
    }


    // for now I did it with a new thread for every connection, however that's porbably no toto scvalable
    public void run() throws IOException {
        // listen and accept connections
        try (final var server = new ServerSocket(port)) { // binds and listens for TCP connections
            System.err.println("Server listening on port " + port); 
            while (true) {
                final Socket socket = server.accept(); // blocks until a connection is accepted
                // handle each connection in its own thread to keep things simple
                new Thread(() -> { // create a new thread for each connection
                    try {
                        handleConnection(socket);
                    } catch (Throwable t) {
                        System.err.println("Connection handler error: " + t.getMessage());
                        try {
                            socket.close(); // closing the socket on error
                        } catch (IOException ignore) {
                        }
                    }
                }, "gemini-conn").start(); // starts the thread
            }
        }
    }

    private void handleConnection(Socket socket) throws IOException, ProtocolSyntaxException {
        try (socket; final InputStream in = socket.getInputStream(); final OutputStream out = socket.getOutputStream()) {
            String requested;
            try {
                requested = Wire.readHeaderLine(in);
            } catch (IOException e) {
                // nothing readable — send failure
                final Reply r = new Reply(59, "Read error");
                r.writeTo(out);
                out.flush();
                return;
            }

            System.err.println("Server received: " + requested);

            try {
                final Request req = Request.parse(new java.io.ByteArrayInputStream((requested + Wire.CRLF).getBytes(StandardCharsets.UTF_8)));

                final String body = "Hello from Server\nYou requested: " + req.getURI().toString() + "\n";
                final Reply ok = new Reply(20, "text/plain");
                ok.writeTo(out);
                out.write(body.getBytes(StandardCharsets.UTF_8));
                out.flush();
            } catch (ProtocolSyntaxException | URISyntaxException e) {
                final Reply bad = new Reply(59, "Bad request");
                bad.writeTo(out);
                out.flush();
            }
        }
    }
}
