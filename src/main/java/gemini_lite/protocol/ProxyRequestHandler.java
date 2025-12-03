package gemini_lite.protocol;

import java.io.InputStream;
import java.net.URI;

import gemini_lite.engine.ClientEngine;

/**
 * Handles Gemini-Lite proxy requests -> following redirects
 */
public class ProxyRequestHandler implements RequestHandler {
    private static final int MAX_REDIRECTS = 5;
    private static final long RETRY_DELAY_MS = 1000L;  // delay (in msec) before retrying after a temporary failure

    private final ClientEngine engine;

    public ProxyRequestHandler(ClientEngine engine) {
        if (engine == null){
            throw new NullPointerException("engine");
        }
        this.engine = engine;
    }

    /**
     * handles an incoming request, following redirects and retrying
     * repeating failures up to certain limits (so it doesn't go on forever)
     * @param request The request to process.
     * @return A HandlerResult containing the remote server reply and body.
     */
    @Override
    public HandlerResult handle(Request request) throws Exception {
        URI target = request.getURI();
        int redirectCount = 0; // keep track of redirects (as max is 5)
        while (true) {  // loop until a valid reply is obtained or retry limit is reached
            try {
                ClientEngine reqEngine = new ClientEngine();
                Reply remoteReply = reqEngine.sendRequest(new URI(target.toString()));
                int group = remoteReply.getStatusGroup();

                 if (group == 1 || group == 2) { // status group 1 or 2 —> just return the response
                    return new HandlerResult(remoteReply, reqEngine.getLastResponseBody());
                }

                if (group == 3) { // handle redirection
                    String location = remoteReply.getMessage();

                    if (location.contains(" ") || location.isEmpty()) { // invalid redirect URIs are treated as proxy errors
                        closeResponseBody(reqEngine);
                        return new HandlerResult(new Reply(43, "Proxy error: invalid redirection URI"));
                    }

                    if (++redirectCount > MAX_REDIRECTS) { // is we have more than 5 redirects, we give an error
                        return new HandlerResult(new Reply(50, "Too many redirections (limit is 5 ;( )"));
                    }

                    closeResponseBody(reqEngine);
                    target = resolveRedirectUri(target, location);
                    if (target == null) {
                        return new HandlerResult(new Reply(43, "Proxy error: invalid redirection URI"));
                    }
                    continue; // retry with the new location
                }

                 if (group == 4 && remoteReply.getStatus() == 44) {
                    closeResponseBody(reqEngine);
                    Thread.sleep(RETRY_DELAY_MS);
                    continue;
                }
                return new HandlerResult(remoteReply, reqEngine.getLastResponseBody()); // for all other responses, forward reply back to client as-is

            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                return new HandlerResult(new Reply(40, "Server error: interrupted"));
            } catch (Exception e) {
                String msg = e.getMessage() != null ? e.getMessage() : "Unknown proxy error";
                return new HandlerResult(new Reply(43, "Proxy error: " + msg));
            }
        }
    }
    /**
     * Reads and closes the last response body safely.
     * Used to release system resources after a request is done or aborted.
     * @param reqEngine The engine whose last response stream should be closed.
     */
    private void closeResponseBody(ClientEngine reqEngine) {
        try {
            InputStream body = reqEngine.getLastResponseBody();
            if (body != null){
                body.readAllBytes(); // drain remaining data
                body.close();
            }
        } catch (Exception ignored) {
        }
    }
    /**
     * resolves the new redirection URI relative to the current base URI
     * @param base The base URI from the current request.
     * @param newUri The URI string from the redirect response.
     * @return A resolved URI, or null if the value was malformed.
     */
    private URI resolveRedirectUri(URI base, String newUri) {
        try {
            return base.resolve(newUri);
        } catch (Exception e) {
            return null;
        }
    }
}
