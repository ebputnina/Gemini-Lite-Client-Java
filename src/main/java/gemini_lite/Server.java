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

import gemini_lite.protocol.ProtocolSyntaxException;
import gemini_lite.protocol.Reply;
import gemini_lite.protocol.Request;
import gemini_lite.protocol.RequestHandler;


public class Server {
    private final int port;
    private final RequestHandler handler;

    public Server(int port, RequestHandler handler) {
        this.port = port;
        this.handler = handler;
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.err.println("Usage: java gemini_lite.Server <directory> [<port>]");
            System.exit(1);
        }

        final String directory = args[0]; 
        final int port = (args.length > 1) ? Integer.parseInt(args[1]) : 1958;
        new Server(port, new gemini_lite.protocol.FileSystemRequestHandler(directory)).run();
    }



    public void run() throws IOException {
        final ExecutorService exec = Executors.newFixedThreadPool(32);
        try (final ServerSocket server = new ServerSocket(port)) { 
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
                    final Socket socket = server.accept(); 
                    exec.submit(() -> {
                        try {
                            socket.setSoTimeout(5000);
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
            final Request req;
            try {
                req = Request.parse(in);
            } catch (ProtocolSyntaxException | URISyntaxException e) {
                final Reply bad = new Reply(51, "Bad request");
                bad.writeTo(out);
                out.flush();
                return;
            } catch (IOException e) {
                final Reply r = new Reply(59, "Read error");
                r.writeTo(out);
                out.flush();
                return;
            }

            System.err.println("Server received: " + req.getURI() + " from " + socket.getRemoteSocketAddress());

            try {
                final gemini_lite.protocol.HandlerResult result = handler.handle(req);
                final Reply rep = result.getReply();
                rep.writeTo(out);

                if (result.hasBody()) {
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
                final Reply err = new Reply(40, "Server error");
                err.writeTo(out);
                out.flush();
                System.err.println("Handler threw: " + e.getMessage());
            }
        }
    }
}
