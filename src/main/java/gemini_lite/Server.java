package gemini_lite;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.URISyntaxException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import gemini_lite.protocol.*;


/**
 * Can act as a file-serving server or as a proxy depending on the handler type
 */
public class Server {
    private final int port; // port to listen on
    private final RequestHandler handler;
    private final boolean isProxy; // as it can also run as a proxy, flag it if it does

    public Server(int port, RequestHandler handler) {
        this.port = port;
        this.handler = handler;
        this.isProxy = handler instanceof ProxyRequestHandler; // if the handler is a ProxyRequestHandler, the server treats itself as a proxy
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.err.println("Usage: java gemini_lite.Server <directory> [<port>]");
            System.exit(1);
        }

        final String directory = args[0]; // serve files from the specified directory
        final int port = (args.length > 1) ? Integer.parseInt(args[1]) : 1958;
        new Server(port, new gemini_lite.protocol.FileSystemRequestHandler(directory)).run();
    }

    public void run() throws IOException { // main server loop: accept connections and hand them off to a thread pool
        final ExecutorService exec = Executors.newFixedThreadPool(32); // fixed-size thread pool for handling client connections
        try (final ServerSocket server = new ServerSocket(port)) {
            System.err.println((isProxy ? "Proxy" : "Server") + " listening on port " + server.getLocalPort());


            // close the server and threads when the JVM is shutting down
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.err.println("Shutdown requested: closing server socket and thread pool");
                try {
                    server.close();
                } catch (IOException ignored) {
                }
                exec.shutdownNow();
            }));

            while (!exec.isShutdown()) { // keep taking new connections until the executor is shut down
                try {
                    final Socket socket = server.accept(); 
                    exec.submit(() -> {
                        try {
                            socket.setSoTimeout(5000); // avoid hanging connections
                            handleConnection(socket);
                        } catch (Exception e) {
                            System.err.println("Connection handler error: " + e.getMessage());
                            try { socket.close(); } catch (IOException ignore) { }
                        }
                    });
                } catch (SocketException se) {
                    System.err.println("Server socket closed, exiting accept loop");
                    break;
                } catch (IOException e) {
                    System.err.println("I/O error in accept(): " + e.getMessage());
                    break;
                }
            }
        } finally {  // first try to shut down worker threads gracefully, then if that doesnt work -> force
            exec.shutdown();
            try {
                if (!exec.awaitTermination(5, TimeUnit.SECONDS)) exec.shutdownNow();
            } catch (InterruptedException ignored) {
                exec.shutdownNow();
            }
        }
    }

    /**
     * Handle a single client connection:
     *  -> parse the Gemini request
     *  -> pass it to the RequestHandler
     *  -> send back the reply and optional body
     */
    private void handleConnection(Socket socket) throws IOException {
        try (socket; final InputStream in = socket.getInputStream(); final OutputStream out = socket.getOutputStream()) {
            final Request req;
            try {
                req = Request.parse(in); // parse a request from the input stream
            } catch (ProtocolSyntaxException | URISyntaxException e) { // malformed request
                final Reply bad = new Reply(59, "Bad request");
                bad.writeTo(out);
                out.flush();
                return;
            } catch (IOException e) { // I/O error while reading
                final Reply r = new Reply(59, "Read error");
                r.writeTo(out);
                out.flush();
                return;
            }

            if (isProxy) {
                System.err.println("Proxy received: " + req.getURI() + " from " + socket.getRemoteSocketAddress());
            } else {
                System.err.println("Server received: " + req.getURI() + " from " + socket.getRemoteSocketAddress());
            }

            try {
                final gemini_lite.protocol.HandlerResult result = handler.handle(req);  // delegate the request to the handler
                final Reply rep = result.getReply();
                rep.writeTo(out);

                if (result.hasBody()) { // if there's a body, stream it to the client in chunks
                    try (final InputStream body = result.getBody()) {
                        byte[] buffer = new byte[8192];
                        int bytesRead;
                        while ((bytesRead = body.read(buffer)) != -1) {
                            out.write(buffer, 0, bytesRead);
                        }
                    }
                }

                out.flush();
            } catch (Exception e) {
                final Reply err = new Reply(40, "Server error"); // any other failure becomes a generic server error
                err.writeTo(out);
                out.flush();
                System.err.println("Handler threw: " + e.getMessage());
            }
        }
    }
}
