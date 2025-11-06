package gemini_lite.protocol;

// Basically given a parsed Request, reutnr an appropriate Reply
public interface RequestHandler {
    Reply handle(Request request) throws Exception;
}
