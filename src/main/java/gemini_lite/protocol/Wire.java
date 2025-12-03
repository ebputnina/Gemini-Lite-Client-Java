package gemini_lite.protocol;

import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Reads a single CRLF-terminated header line and enforces syntax rules •`ヮ´•
 */
public class Wire {
    public static final String CRLF = "\r\n";
    static final int MAX_HEADER_LINE = 1024;

    /**
     * reads a single header line terminated by CRLF from the input stream
     * enforces that CR must always be followed by LF (no bare LF) and a max header length
     * @param in Input stream positioned at the start of a header line.
     * @return The header line as a UTF-8 string, without the trailing CRLF.
     */
    public static String readHeaderLine(InputStream in) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int prev = -1;
        int current;
        
        while ((current = in.read()) != -1) {
            // check for LF without CR
            if (current == '\n' && prev != '\r') {
                throw new ProtocolSyntaxException("LF without CR");
            }

            // check maximum header length (not counting the final CRLF)
            if (current != '\n') {
                if (buffer.size() > MAX_HEADER_LINE) {
                    throw new ProtocolSyntaxException("Header line too long");
                }
            }
            // add byte to buffer
            buffer.write(current);
            
            // check for CRLF sequence
            if (prev == '\r' && current == '\n') {
                // found end of header line
                byte[] bytes = buffer.toByteArray();
                // remove CRLF (last 2 bytes)
                byte[] lineBytes = new byte[bytes.length - 2];
                System.arraycopy(bytes, 0, lineBytes, 0, bytes.length - 2);
                return new String(lineBytes, StandardCharsets.UTF_8);
            }
            prev = current;
        }
        
        // if we get here, we hit EOF before finding CRLF
        throw new EOFException("EOF before CRLF");
    }
}
