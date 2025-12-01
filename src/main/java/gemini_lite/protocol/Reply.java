package gemini_lite.protocol;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public final class Reply {

    private final int status;
    private final String message;

    public Reply(int status, String message) throws ProtocolSyntaxException {
        if (status < 10 || status > 59) {
            throw new ProtocolSyntaxException("Status out of range: " + status);
        }

        if (message == null) {
            this.message = "";
        } else {
            this.message = message;
        }

        this.status = status;
    }

    public static Reply fromHeaderLine(String line) throws ProtocolSyntaxException {
        if (line == null) {
            throw new ProtocolSyntaxException("Null header line");
        }
        int sp = line.indexOf(' ');
        String codeStr;
        String meta;

        if (sp == -1) {
            codeStr = line;
            meta = "";
        } else {
            codeStr = line.substring(0, sp);
            meta = line.substring(sp + 1);
        }

        int code;
        try {
            code = Integer.parseInt(codeStr);
            if (codeStr.length() != 2) {
                throw new ProtocolSyntaxException("Status code must be two digits: " + codeStr);
            }
        } catch (NumberFormatException e) {
            throw new ProtocolSyntaxException("Non-numeric status: " + codeStr);
        }

        if (code >= 20 && code < 30) {
            if (sp == -1 || meta.isBlank()) {
                throw new ProtocolSyntaxException("Success status requires MIME type metadata");
            }
        }

        for (int i = 0; i < meta.length(); i++) {
            char c = meta.charAt(i);
            if (c < 0x20 || c == 0x7F || c >= 0x80) {
                throw new ProtocolSyntaxException("Invalid control character in meta");
            }
        }

        if (meta.length() > 1024) {
            throw new ProtocolSyntaxException("Meta field too long");
        }

        return new Reply(code, meta);
    }

    public static Reply parse(InputStream in) throws IOException {
        String line = Wire.readHeaderLine(in);
        return fromHeaderLine(line);
    }


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

