package gemini_lite;

import java.net.Socket;

public class Client {
    public static void main(String[] args) throws Throwable {
        try (final var s = new Socket("demo.svc.leastfixedpoint.nl", 1958)) {
            final var i = s.getInputStream(); // used for reading byte based data, one byte at a time
            final var o = s.getOutputStream(); // used for writing byte based data, raw bytes
        }
    }
}
