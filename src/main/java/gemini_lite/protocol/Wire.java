package gemini_lite.protocol;

import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class Wire {
    public static final String CRLF = "\r\n";
    static final int MAX_HEADER_LINE = 1024;

    public static String readHeaderLine(InputStream in) throws IOException, ProtocolSyntaxException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int prev = -1;
        int current;
        
        while ((current = in.read()) != -1) {
            // check for LF without CR
            if (current == '\n' && prev != '\r') {
                throw new ProtocolSyntaxException("LF without CR");
            }
            
            // add byte to buffer
            buffer.write(current);
            
            // check for CRLF sequence
            if (prev == '\r' && current == '\n') {
                // found end of header line
                byte[] bytes = buffer.toByteArray();
                // Remove CRLF (last 2 bytes)
                byte[] lineBytes = new byte[bytes.length - 2];
                System.arraycopy(bytes, 0, lineBytes, 0, bytes.length - 2);
                return new String(lineBytes, StandardCharsets.UTF_8);
            }
            
            // Check maximum header length (not counting the final CRLF)
            if (buffer.size() > MAX_HEADER_LINE) {
                throw new ProtocolSyntaxException("Header line too long");
            }
            
            prev = current;
        }
        
        // if we get here, we hit EOF before finding CRLF
        throw new EOFException("EOF before CRLF");
    }
}
