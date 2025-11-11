package gemini_lite;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import gemini_lite.protocol.ProtocolSyntaxException;
import gemini_lite.protocol.Reply;
import gemini_lite.protocol.Request;
import gemini_lite.protocol.RequestHandler;
import gemini_lite.protocol.Wire;


public class Server {
    private final int port;
    private final RequestHandler handler;

    public Server(int port, RequestHandler handler) {
        this.port = port;
        this.handler = handler;
    }

    public static void main(String[] args) throws Exception {
        final int port = (args.length > 0) ? Integer.parseInt(args[0]) : 1958; // if the user passed a port, use it, if not, default to 1958
        new Server(port, new gemini_lite.protocol.FileSystemRequestHandler()).run();
    }


    // Run the server: accept connections and dispatch to a thread-pool.
    public void run() throws IOException {
        final ExecutorService exec = Executors.newFixedThreadPool(32);
        try (final ServerSocket server = new ServerSocket(port)) { // binds and listens for TCP connections
            System.err.println("Server listening on port " + server.getLocalPort());

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.err.println("Shutdown requested: closing server socket and thread pool");
                try {
                    server.close();
                } catch (IOException ignored) {
                }
                exec.shutdownNow();
            }));

            while (!exec.isShutdown()) {
                try {
                    final Socket socket = server.accept(); // blocks until a connection is accepted
                    exec.submit(() -> {
                        try {
                            // per-connection timeout to avoid hanging handlers
                            socket.setSoTimeout(5000);
                            handleConnection(socket);
                        } catch (Exception e) {
                            System.err.println("Connection handler error: " + e.getMessage());
                            try { socket.close(); } catch (IOException ignore) { }
                        }
                    });
                } catch (SocketException se) {
                    // ServerSocket was closed (likely due to shutdown); exit loop
                    System.err.println("Server socket closed, exiting accept loop");
                    break;
                } catch (IOException e) {
                    System.err.println("I/O error in accept(): " + e.getMessage());
                    break;
                }
            }
        } finally {
            exec.shutdown();
            try {
                if (!exec.awaitTermination(5, TimeUnit.SECONDS)) exec.shutdownNow();
            } catch (InterruptedException ignored) {
                exec.shutdownNow();
            }
        }
    }

    private void handleConnection(Socket socket) throws IOException {
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

            System.err.println("Server received: " + requested + " from " + socket.getRemoteSocketAddress());

            try {
                final Request req = Request.parse(new java.io.ByteArrayInputStream((requested + Wire.CRLF).getBytes(StandardCharsets.UTF_8)));
                final Reply rep = handler.handle(req); // 
                rep.writeTo(out); // write reply header

                // if success (20), handler may have prepared body elsewhere
                out.flush();
            } catch (ProtocolSyntaxException | URISyntaxException e) {
                final Reply bad = new Reply(59, "Bad request");
                bad.writeTo(out);
                out.flush();
            } catch (Exception e) {
                final Reply err = new Reply(40, "Server error"); // respond with server error
                err.writeTo(out);
                out.flush();
                System.err.println("Handler threw: " + e.getMessage());
            }
        }
    }
}
