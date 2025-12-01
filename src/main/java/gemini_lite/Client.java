package gemini_lite;
import java.io.Console;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

import gemini_lite.engine.ClientEngine;
import gemini_lite.protocol.ProtocolSyntaxException;
import gemini_lite.protocol.Reply;

public class Client {
    final ClientEngine engine = new ClientEngine();
    private int redirectCount = 0;  // has to be under 6
    private URI currentUri;
    private final String input;
    private int slowDownCount = 0;

    public Client(String input) {
        this.input = input;
    }
    public void setProxy(String host, int port) {
        this.engine.setProxy(host, port);
    }

    /**
     * Entry point: parse command-line arguments, configure proxy if needed, then start the client
     * Arguments:
     *   [0] URL - The Gemini URI to connect to
     *   [1] Input - Optional user text the server asks for
     */
    public static void main(String[] args) {
        // we need at least a URL -> if it's missing, print how to use it and wuit
        if (args.length < 1) {
            System.err.println("Usage: gemini_lite.Client <URL> [<input>]");
            System.exit(1);
        }
        try {
            String url = args[0]; // first argument is always the URL
            String cmdLineInput; // optional second argument: text to send as "input" to the server
            if (args.length > 1) {
                cmdLineInput = args[1];
            } else {
                cmdLineInput = null;
            }

            Client client = new Client(cmdLineInput);

            String proxyEnv = System.getenv("GEMINI_LITE_PROXY");
            if (proxyEnv != null && !proxyEnv.isEmpty()) {
                final String[] parts = proxyEnv.split(":" , 2);
                if (parts.length == 2) {
                    try {
                        final String ph = parts[0]; // proxy host name
                        final int pp = Integer.parseInt(parts[1]); // proxy port number
                        client.setProxy(ph, pp);
                    } catch (NumberFormatException nfe) {
                        System.err.println("Invalid GEMINI_LITE_PROXY port: " + proxyEnv);
                        System.exit(0);
                    }
                } else {
                    System.err.println("Invalid GEMINI_LITE_PROXY format, expected host:port");
                    System.exit(0);
                }
            }

            client.run(new URI(url));
        } catch (ProtocolSyntaxException e){
            System.err.println("Protocol/Network Error or Abrupt Disconnections: " + e.getMessage());
            System.exit(1);
        } catch (Exception e) {
            System.err.println("Unexpected Error: " + e.getMessage());
            System.exit(1);
        }
    }

    public void run(URI initialUri) throws Exception {
        this.currentUri = initialUri;

        while (true) {
            Reply reply = engine.sendRequest(this.currentUri); // send the current URI and get the server's response
            boolean continueLoop = processReply(reply); // process the response
            if (!continueLoop) {
                break;
            }
        }
    }

    /**
     * Route the response to the appropriate handler based on the Gemini status code
     */
    private boolean processReply(Reply reply) throws IOException {
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

    /**
     * Status 1x: the server is asking for input
     * Status 11 is sensitive input (password) -> use readPassword instead of readLine
     */
    private boolean handleInput(Reply reply) throws ProtocolSyntaxException {
        System.err.println("Status " + reply.getStatus() + ": Input requested (" + reply.getMessage() + ")");
        Console console = System.console();
        String inputHere;
        // if the user already gave input on the command line, reuse that
        if (input != null){
            System.err.println("Using command line input: " + input);
            inputHere = this.input;
        }else if (console != null){
            if (reply.getStatus() == 11){  // status 11 = sensitive input -> do not echo as user types.
                inputHere = new String(console.readPassword("Enter input for '" + reply.getMessage() + "': "));
            }else{ // normal input -> read a visible line of text.
                inputHere = console.readLine("Enter input for '" + reply.getMessage() + "': ");
            }
        }else{
            throw new ProtocolSyntaxException("Cannot read input for Status " + reply.getStatus());
        }
        // there should be smth about "t should be URI-encoded per [STD66] and sent as a query to the same URI that generated this response."
        this.currentUri = engine.toQuery(this.currentUri, inputHere);
        return true;
    }

    /**
     * Status 2x: Success
     * Display the response body
     * If the content is Gemini text format, colorize it for readability.
     * Otherwise, stream binary data as-is (images, documents, etc).
     */
    private boolean handleSuccess(Reply reply) throws IOException {
        System.err.println("20 Success: " + reply.getMessage());
        InputStream responseBody = engine.getLastResponseBody();
        if (responseBody == null){
            throw new IOException("The response body is null.");
        }

        if (reply.getMessage().startsWith("text/gemini")) {
            try (java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(responseBody))) {
                String line;
                while ((line = br.readLine()) != null) {
                    System.out.println(colorizeGemtextLine(line));
                }
            }
        } else {
            byte[] buffer = new byte[4096]; // if its non-gemtext copy the raw bytes directly to stdout
            int bytesRead;

            while ((bytesRead = responseBody.read(buffer)) != -1) {
                System.out.write(buffer, 0, bytesRead);
            }
            System.out.flush();
        }
        System.exit(0); // client exits
        return false;
    }

    /**
     * Status 3x: the server is redirecting us to a different URI
     * limited to 5 hops maximum
     */
    private boolean handleRedirection(Reply reply) throws ProtocolSyntaxException {
        if (redirectCount >= 5) {
            System.err.println("1 Too many redirections (limit is 5).");
            System.exit(1);
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

    /**
     * Status 4x: A temporary failure occurred (try again, maybe later)
     * Status 44 is special: the server is asking us to slow down
     */
    private boolean handleTemporaryFailure(Reply reply) {
        if (reply.getStatus() == 44) {
            // slowDownCount is a counter used to prevent infinite loops, otherwise it never stops
            slowDownCount++;
            if (slowDownCount > 5) {
                System.err.println("Error: Slow down repeated too many times. Giving up. :(");
                System.exit(44);
            }
            try {
                int slowDownSeconds = Integer.parseInt(reply.getMessage());
                System.err.println("Status 44: Slow down. Waiting " + slowDownSeconds + " seconds...");
                Thread.sleep(slowDownSeconds * 1000L); // slow down for as many seconds as mentioned in the message
                return true;
            } catch (Exception e) {
                System.err.println(reply.getMessage());
                System.exit(44);
            }
        }

        System.err.println("Error: " + reply.getStatus() + " Temporary Failure: " + reply.getMessage());
        System.exit(reply.getStatus());
        return false;
    }

    /**
     * Status 5x: a permanent failure
     * The request cannot be retried; exit with the error status.
     */
    private boolean handlePermanentFailure(Reply reply) {
        System.err.println("Error: " + reply.getStatus() + " Permanent Failure: " + reply.getMessage());
        System.exit(reply.getStatus());
        return false;
    }

    /**
     * Apply terminal colors to Gemini text markup for prettier display ⋆˚✿˖° 𐙚 ₊ ⊹ ♡.
     */
    private String colorizeGemtextLine(String line) {
        final String RESET = "\u001B[0m";
        final String C054 = "\u001B[38;5;54m";
        final String C060 = "\u001B[38;5;60m";
        final String C066 = "\u001B[38;5;66m";
        final String C072 = "\u001B[38;5;72m";
        final String C078 = "\u001B[38;5;78m";
        final String C084 = "\u001B[38;5;84m";

        if (line.startsWith("###")){
            return C066 + line + RESET;
        }
        if (line.startsWith("##")){
            return C060 + line + RESET;
        }
        if (line.startsWith("#")){
            return C054 + line + RESET;
        }
        if (line.startsWith("=>")){
            return C072 + line + RESET;
        }
        if (line.startsWith("*")){
            return C078 + line + RESET;
        }
        if (line.startsWith(">")){
            return C084 + line + RESET;
        }
        return line;
    }

}

