package gemini_lite.protocol;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * Represents a single reply line (status + meta/message).
 */
public final class Reply {

    private final int status;  // status code in the range 10–59
    private final String message;

    /**
     * creates a Reply with a status code and message
     * @param status  Gemini status code (10–59).
     * @param message Meta string; null is treated as empty.
     * @throws ProtocolSyntaxException if status is outside 10–59.
     */
    public Reply(int status, String message) throws ProtocolSyntaxException {
        if (status < 10 || status > 59) {
            throw new ProtocolSyntaxException("Status out of range: " + status);
        }

        if (message == null) { // ensure that there is at least an empty string in the message -> ensure it is never null
            this.message = "";
        } else {
            this.message = message;
        }

        this.status = status;
    }

    /**
     * parses a header line like "20 text/gemini" into a Reply
     * @param line Raw status line from the wire (without CRLF).
     * @return Parsed Reply instance.
     */
    public static Reply fromHeaderLine(String line) throws ProtocolSyntaxException {
        if (line == null) {
            throw new ProtocolSyntaxException("Null header line");
        }
        int sp = line.indexOf(' ');
        String codeStr;
        String meta;

        if (sp == -1) { // split into status code and meta
            codeStr = line;
            meta = "";
        } else {
            codeStr = line.substring(0, sp);
            meta = line.substring(sp + 1);
        }

        int code;
        try {
            code = Integer.parseInt(codeStr);
            if (codeStr.length() != 2) { // status codes are 2 digits long
                throw new ProtocolSyntaxException("Status code must be two digits: " + codeStr);
            }
        } catch (NumberFormatException e) {
            throw new ProtocolSyntaxException("Non-numeric status: " + codeStr);
        }

        // success replies (2x) must include non-blank MIME type metadata
        if (code >= 20 && code < 30) {
            if (sp == -1 || meta.isBlank()) {
                throw new ProtocolSyntaxException("Success status requires MIME type metadata");
            }
        }

        return new Reply(code, meta);
    }

    // reads a reply from an InputStream, consuming exactly one header line
    public static Reply parse(InputStream in) throws IOException {
        String line = Wire.readHeaderLine(in);
        return fromHeaderLine(line);
    }

    // writes this reply to an OutputStream using Gemini wire format
    public void writeTo(OutputStream out) throws IOException {
        String wireline = status + " " + message + Wire.CRLF;
        out.write(wireline.getBytes(StandardCharsets.UTF_8));
        out.flush();
    }

    public int getStatus(){
        return status;
    }
    public int getStatusGroup(){
        return status / 10;
    }
    public String getMessage(){
        return message;
    }
}

