package gemini_lite.protocol;

import java.io.InputStream;

/**
 * Contains the reply (status + meta) and optional response body stream.
 */
public class HandlerResult {
    private final Reply reply;
    private final InputStream body;

    public HandlerResult(Reply reply, InputStream body) {
        if (reply == null) throw new NullPointerException("reply");
        this.reply = reply;
        this.body = body;
    }

    /**
     * creates a result with reply only (no body)
     * @param reply the reply (status code + meta)
     * @throws NullPointerException if reply is null
     */
    public HandlerResult(Reply reply) {
        this(reply, null);
    }

    /**
     * gets the reply containing status code and meta information
     * @return the reply object
     */
    public Reply getReply() {
        return reply;
    }

    public InputStream getBody() {
        return body;
    } // get the body if presnet

    public boolean hasBody() {
        return body != null;
    } // checks if this result has a response body
}
