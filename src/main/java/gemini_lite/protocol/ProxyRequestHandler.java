package gemini_lite.protocol;

import java.io.InputStream;
import java.net.URI;

import gemini_lite.engine.ClientEngine;

public class ProxyRequestHandler implements RequestHandler {
    private static final int MAX_REDIRECTS = 5;
    private static final long RETRY_DELAY_MS = 1000L;

    private final ClientEngine engine;

    public ProxyRequestHandler(ClientEngine engine) {
        if (engine == null){
            throw new NullPointerException("engine");
        }
        this.engine = engine;
    }

    @Override
    public HandlerResult handle(Request request) throws Exception {
        URI target = request.getURI();
        int redirectCount = 0;
        while (true) {
            try {
                ClientEngine reqEngine = new ClientEngine();
                Reply remoteReply = reqEngine.sendRequest(target);
                int group = remoteReply.getStatusGroup();

                 if (group == 1 || group == 2) {
                    return new HandlerResult(remoteReply, reqEngine.getLastResponseBody());
                }

                if (group == 3) {
                    String location = remoteReply.getMessage();

                    if (location.contains(" ") || location.isEmpty()) {
                        closeResponseBody(reqEngine);
                        return new HandlerResult(new Reply(43, "Proxy error: invalid redirection URI"));
                    }

                    if (++redirectCount > MAX_REDIRECTS) {
                        return new HandlerResult(new Reply(50, "Too many redirections (limit is 5 ;( )"));
                    }

                    closeResponseBody(reqEngine);
                    target = resolveRedirectUri(target, location);
                    if (target == null) {
                        return new HandlerResult(new Reply(43, "Proxy error: invalid redirection URI"));
                    }
                    continue;
                }

                 if (group == 4 && remoteReply.getStatus() == 44) {
                    closeResponseBody(reqEngine);
                    Thread.sleep(RETRY_DELAY_MS);
                    continue;
                }
                return new HandlerResult(remoteReply, reqEngine.getLastResponseBody());

            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                return new HandlerResult(new Reply(40, "Server error: interrupted"));
            } catch (Exception e) {
                String msg = e.getMessage() != null ? e.getMessage() : "Unknown proxy error";
                return new HandlerResult(new Reply(43, "Proxy error: " + msg));
            }
        }
    }
    private void closeResponseBody(ClientEngine reqEngine) {
        try {
            InputStream body = reqEngine.getLastResponseBody();
            if (body != null){
                body.readAllBytes();
                body.close();
            }
        } catch (Exception ignored) {
        }
    }
    private URI resolveRedirectUri(URI base, String newUri) {
        try {
            return base.resolve(newUri);
        } catch (Exception e) {
            return null;
        }
    }
}
