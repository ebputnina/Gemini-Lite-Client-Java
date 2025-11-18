package gemini_lite;
import java.net.URI;

import gemini_lite.engine.ClientEngine;
import gemini_lite.protocol.Reply;

public class Client {
    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            System.err.println("Usage: gemini_lite.Client gemini-lite://host[:port]/path");
            System.exit(2); // exit code 2 = “bad usage”
        }

        final URI uri = new URI(args[0]);
        final ClientEngine engine = new ClientEngine();

        try {
            Reply reply = engine.sendRequest(uri); 
            System.out.println(reply.getStatus() + " " + reply.getMessage());
        } catch (Exception e) {
                    // Windows may report: "An established connection was aborted by the software in your host machine" :(
                    System.err.println("Unespected error: " + e.getMessage());
                    System.exit(1);
        } 
    }
}

// TODO: exception handling??

