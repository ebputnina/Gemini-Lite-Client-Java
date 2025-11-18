package gemini_lite;
import java.io.IOException;
import java.net.URI;

import gemini_lite.engine.ClientEngine;
import gemini_lite.protocol.ProtocolSyntaxException;
import gemini_lite.protocol.Reply;

public class Client {
    final ClientEngine engine = new ClientEngine();
    // private int redirectCount = 0;  has to be under 6
    private URI currentUri;

    public static void main(String[] args) throws Exception {
        if (args.length > 0) {
            System.err.println("Usage: gemini_lite.Client <URL> [<input>]");
            System.exit(1);
        }
        try {
            String url = args[0];
            new Client().run(new URI(url));
        } catch (ProtocolSyntaxException e){
            System.err.println("Local Protocol/Network Error or Abrupt Disconnections: " + e.getMessage());
            System.exit(1);
        } catch (Exception e) {
            System.err.println("Unexpected Application Error: " + e.getMessage());
            System.exit(1);
        }
    }

    public void run(URI initialUri) throws Exception {
        this.currentUri = initialUri;

        while (true) {
            Reply reply = engine.sendRequest(this.currentUri);
            boolean continueLoop = processReply(reply);
            if (!continueLoop) {
                break;
            }
        }
    }

    private boolean processReply(Reply reply) throws ProtocolSyntaxException, IOException {
        int status = reply.getStatus();
        switch (reply.getStatusGroup()) {
            case 1:
                return handleInput(reply);
            case 2:
                return handleSuccess(reply);
            case 3:
                return handleRedirection(reply);
            case 4:
                return handleTemporaryFailure(reply);
            case 5:
                return handlePermanentFailure(reply);
            default:
                throw new ProtocolSyntaxException("Unhandled status group: " + status);
        }
    }

    private boolean handleInput(Reply reply) throws ProtocolSyntaxException {
        return false;
    }

    private boolean handleSuccess(Reply reply) throws IOException {
        return false;
    }

    private boolean handleRedirection(Reply reply) throws ProtocolSyntaxException {
        return false;
    }

    private boolean handleTemporaryFailure(Reply reply) {
        return false;
    }

    private boolean handlePermanentFailure(Reply reply) {
        return false;
    }
}

