package gemini_lite;
import java.io.Console;
import java.io.IOException;
import java.net.URI;

import gemini_lite.engine.ClientEngine;
import gemini_lite.protocol.ProtocolSyntaxException;
import gemini_lite.protocol.Reply;

public class Client {
    final ClientEngine engine = new ClientEngine();
     private int redirectCount = 0;  // has to be under 6
    private URI currentUri;
    private static String input;

    public Client(String input) {
        this.input = input;
    }

    public static void main(String[] args) throws Exception {
        if (args.length > 0) {
            System.err.println("Usage: gemini_lite.Client <URL> [<input>]");
            System.exit(1);
        }
        try {
            String url = args[0];
            Client client = new Client(input);
            client.run(new URI(url));
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
        System.err.println("Status " + reply.getStatus() + ": Input requested (" + reply.getMessage() + ")");
        Console console = System.console();
        String inputHere;
        if (input != null){
            System.err.println("Using command line input: " + input);
            inputHere = input;
        }else if (console != null){
            if (reply.getStatus() == 11){
                input = new String(console.readPassword("Enter input for '" + reply.getMessage() + "': "));
            }else{
                input = console.readLine("Enter input for '" + reply.getMessage() + "': ");
            }
        }else{
            throw new ProtocolSyntaxException("Cannot read input for Status " + reply.getStatus());
        }
        // there should be smth about "t should be URI-encoded per [STD66] and sent as a query to the same URI that generated this response."
        return true;
    }

    private boolean handleSuccess(Reply reply) throws IOException {
        System.err.println("20 Success: " + reply.getMessage());
        System.exit(0);
        return false;
    }

    private boolean handleRedirection(Reply reply) throws ProtocolSyntaxException {
        if (redirectCount >= 5) {
            System.err.println("50 Permanent failure: Too many redirections (limit is 5).");
            System.exit(50);
            return false;
        }
        String newUri = reply.getMessage();
        try{
            this.currentUri = this.currentUri.resolve(newUri);
        }catch(Exception e){
            throw new ProtocolSyntaxException("Protocol error: Invalid redirection URI: " + newUri);
        }
        redirectCount++;
        System.err.println("Status " + reply.getStatus() + ": Redirecting (count " + redirectCount + ") to " + this.currentUri);
        return true;
    }

    private boolean handleTemporaryFailure(Reply reply) {
        System.err.println("Error: " + reply.getStatus() + " Temporary Failure: " + reply.getMessage());
        if (reply.getStatus() == 44){
            System.err.println("Status 44: Server requests slow down.");
        }
        System.exit(reply.getStatus());
        return false;
    }

    private boolean handlePermanentFailure(Reply reply) {
        System.err.println("Error: " + reply.getStatus() + " Permanent Failure: " + reply.getMessage());
        System.exit(reply.getStatus());
        return false;
    }
}

