package gemini_lite.protocol;

import java.io.InputStream;

public class HandlerResult {
    private final Reply reply;
    private final InputStream body;

    public HandlerResult(Reply reply, InputStream body) {
        if (reply == null) throw new NullPointerException("reply");
        this.reply = reply;
        this.body = body;
    }

    public HandlerResult(Reply reply) {
        this(reply, null);
    }

    public Reply getReply() {
        return reply;
    }

    public InputStream getBody() {
        return body;
    }

    public boolean hasBody() {
        return body != null;
    }
}
